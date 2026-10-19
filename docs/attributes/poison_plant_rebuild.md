# Poison Plant Magic rebuild (1.93.0)

Magic type `MagicType.POISON_PLANT`, grimoire book `book_poison_plant`, painter `Aura.POISON_PLANT`, props `PropKind.POISON_PLANT_1`, VFX layer `PoisonPlantLayer`. Self-tests `magic.tree_rebuild_pages`, `magic.tree_rebuild_assets`, `magic.tree_rebuild_props` (registered once for all six nature magics); JUnit `TreeRulesTest`.

## NEW / REPLACED / KEPT

| Part | State | Notes |
|---|---|---|
| Temporary armour (Venomflora armour) | NEW | No armour set existed for this magic. Added next to everything else. |
| Summon / construct models | NEW | poison_plant_bloom, poison_plant_maw |
| Page ids, order, tiers, mastery bits | KEPT | Every old id stays at its old position; new pages are appended last (`TreeRulesTest` checks this). |
| Texts and numbers of the five generic pages | REPLACED | The generic placeholder spells of the book are remade as Poison Plant Magic spells; the ids stay. |
| Page list | NEW | Pages appended: trap_maw, spore_burst, antivenom_graft, pitcher_swallow, thorn_volley |
| VFX | NEW | Three new shapes POISON_PLANT_FX1, POISON_PLANT_FX2, POISON_PLANT_FX3 (appended to `VfxShape`); the old `WIKI_MAGIC_*` shapes stay registered and are still used by the book for the cast circle. |
| Sprites | NEW | Eight coloured particle sprites `textures/particle/poison_plant_*.png`. |
| Time Magic files, World Tree / Briar / Cherry Blossom / Fungus files | KEPT | Not touched. |

## Canon sources

- Black Clover: Poison Plant Magic creates and controls toxic plants. The canon description known: vines that skewer targets and tunnel through the ground, and plants that release a dense cloud of gaseous poison. Burrowing Venom Vine and Miasma Bloom follow that description; the rest is the nearest version. The wiki page itself was not reachable.
- Look: violet and acid-green flora, glowing spore pods, carnivorous traps, venom drops.
- The wiki was unreachable from the build sandbox. What is stated above as canon is only what was known without the page.

## Armour: Venomflora armour (`Aura.POISON_PLANT`)

Eleven GeoLite pieces (`tools/gen_poison_plant_rebuild_geo.py`) that lunge in from the sides like snapping traps. Delay 0 to 12 ticks per piece, forge-in over 8 ticks, shatter over the last 18 ticks, the same `EquipAnimation` as the Djinn Full Equip. First person hides the head pieces; the pieces drop detail at 24 and 56 blocks.

| Piece | Part | Delay (ticks) | Flies in | Model |
|---|---|---|---|---|
| helm | head | 0 | from the sides | flytrap hood whose two toothed lobes open and snap |
| crown | head | 12 | from above | ring of glowing spore pods (detail) |
| chest | body | 2 | from the front | three swollen gourd pods with glowing veins (glow) |
| back | body | 4 | from behind | a pitcher plant with an opening lid and a venom pool |
| pauld_r / pauld_l | arms | 3 / 5 | from the sides | spiked pod pauldrons |
| bracer_r / bracer_l | arms | 6 / 8 | from the sides | toxic leaf bracers with hooked venom claws |
| greave_r / greave_l | legs | 9 / 11 | from the sides | bulb greaves |
| aux | body | 12 | from below | four low spore pods (detail) |

Sprites: rising spores, dripping venom, a toxic sigil (additive). Style colour 0xB8FF30.

Render passes: skin overlay (cutout, translucent while fading), pieces (cutout, glow additive), skin glow (additive), sprites (additive). Softening 0.92 of every blow while it runs (10 s); whoever strikes the wearer is poisoned.

Preview cost: about 6440 vertices for the full set in the preview (advisory budget 6000), above the advisory budget.

## Summon and construct models

| Model | Prop | Size | Clips | What it is |
|---|---|---|---|---|
| poison_plant_bloom | POISON_PLANT_1 variant 0 | 12 bones, 33 cubes | idle, open | a poison bloom that opens and breathes a gas cloud (3 to 4 s of the zone); used by Miasma Bloom and Toxic Garden. |
| poison_plant_maw | POISON_PLANT_1 variant 1 | 9 bones, 36 cubes | idle, snap | a carnivorous trap with a toothed maw that snaps on the first foe near it, again every 2.5 s; used by Trap Maw (12 s) and Toxic Garden. |

GeoLite, box UV with an emissive `_glow` pass, checked by `tools/geo_check.py` and `GeoAssetsTest`. Painted by `client/prop/PoisonPlantPropPainter` (`TreePropPainter` helpers). Summons are bound to the caster, can be hit and expire; the variant is the low byte of the prop param, the phase bits 8-9, the clock bits 10-15.

## Spells

Damage numbers are the base values given to the grimoire's `hurt` (scaled by the book, mastery and EP). Control follows `BalanceLaw.controlTicks` (bosses are capped, hard controls share the 12 second lock).

| Page id | Name | State | Numbers |
|---|---|---|---|
| attribute_bolt | Venom Thorn | KEPT id, remade | starter: bolt 9 damage, pierces, venom wound |
| attribute_cut | Burrowing Venom Vine | KEPT id, remade | mid: vine tunnels along the ground and erupts, 9 damage, thrown up, poisoned |
| attribute_field | Miasma Bloom | KEPT id, remade | zone: 9 s gas cloud, poison, blindness |
| attribute_domain | Toxic Garden | KEPT id, remade | signature: three blooms and two maws, 10 s, 3 damage per second, cooldown 45 s |
| attribute_guard | Venomflora Armor | KEPT id, remade | mid: Venomflora armour 10 s |
| trap_maw | Trap Maw | NEW | mid: maw summon 12 s |
| spore_burst | Spore Burst | NEW | mid: cone, 4 damage, blind, nausea, poison |
| antivenom_graft | Antivenom Graft | NEW | mid: cleanses harmful effects of the caster and friends near, heals 12 percent of max health, cooldown 15 s |
| pitcher_swallow | Pitcher Swallow | NEW | mid: hard control on one foe (shares the 12 s lock), 2.5 damage per 0.25 s tick window, drain |
| thorn_volley | Toxic Thorn Volley | NEW | mid: five homing venom thorns |

Lang: `tools/lang/poison_plant_rebuild.json` (book description and a description for every page). Page names are literal strings in `book/PoisonPlantArts.java`.

## VFX

| Shape | Use | Look |
|---|---|---|
| POISON_PLANT_FX1 | cast | Venom Thorn: a toxic sigil contracts on the caster while spores are drawn in, a hooked fang flies in a trail of venom drops and gas puffs |
| POISON_PLANT_FX2 | zone | Miasma: the sigil turns on the ground, thorny vines topped with spore pods grow round the rim, great gas puffs drift across the zone, spores rise |
| POISON_PLANT_FX3 | impact | Toxic Bloom: a flash and a spiked star, a ring of fangs rises round the centre and closes in, a wall of gas runs out, spores and drops fly |

`vfx/client/layer/PoisonPlantLayer.java` on the shared blocks of `TreeFx` (ground rings, standing shapes, strands, wind, debris, flash). Each shape stays inside the 400-vertex budget (80 to 184 used). Preview: `python tools/vfx_preview/preview.py` with the scenes in `tools/vfx_preview/scenes.d/poison_plant_rebuild.json`; armour and summons: `python tools/render_preview/preview.py` with `tools/render_preview/scenes.d/poison_plant_rebuild.json`.

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
