# Tree Magic rebuild (1.93.0)

Magic type `MagicType.TREE`, grimoire book `book_tree`, painter `Aura.TREE`, props `PropKind.TREE_1`, VFX layer `TreeLayer`. Self-tests `magic.tree_rebuild_pages`, `magic.tree_rebuild_assets`, `magic.tree_rebuild_props` (registered once for all six nature magics); JUnit `TreeRulesTest`.

## NEW / REPLACED / KEPT

| Part | State | Notes |
|---|---|---|
| Temporary armour (Heartwood armour) | NEW | No armour set existed for this magic. Added next to everything else. |
| Summon / construct models | NEW | tree_guardian, tree_lance |
| Page ids, order, tiers, mastery bits | KEPT | Every old id stays at its old position; new pages are appended last (`TreeRulesTest` checks this). |
| Texts and numbers of the five generic pages | REPLACED | The generic placeholder spells of the book are remade as Tree Magic spells; the ids stay. |
| Page list | NEW | Pages appended: heartwood_guardian, seedling_volley, timber_wall, verdant_canopy, heartwood_fist |
| VFX | NEW | Three new shapes TREE_FX1, TREE_FX2, TREE_FX3 (appended to `VfxShape`); the old `WIKI_MAGIC_*` shapes stay registered and are still used by the book for the cast circle. |
| Sprites | NEW | Eight coloured particle sprites `textures/particle/tree_fx_*.png`. |
| Time Magic files, World Tree / Briar / Cherry Blossom / Fungus files | KEPT | Not touched. |

## Canon sources

- Black Clover: Tree Magic is the attribute of creating and controlling trees and wood (the grimoires of the World Tree family). No wiki page of it was reachable; the spells are built from the name and what the magic is.
- Look: bark, heartwood with golden tree-rings, living branches, leaves, acorns.
- The wiki was unreachable from the build sandbox. What is stated above as canon is only what was known without the page.

## Armour: Heartwood armour (`Aura.TREE`)

Eleven GeoLite pieces (`tools/gen_tree_rebuild_geo.py`) forged in and shattered out through `client/aura/EquipAnimation`, over a bark skin overlay. Delay 0 to 12 ticks per piece, forge-in over 8 ticks, shatter over the last 18 ticks, the same `EquipAnimation` as the Djinn Full Equip. First person hides the head pieces; the pieces drop detail at 24 and 56 blocks.

| Piece | Part | Delay (ticks) | Flies in | Model |
|---|---|---|---|---|
| helm | head | 0 | from above | bark helm with antlers of living branches |
| crown | head | 12 | from above | floating ring of leaves (glow, detail) |
| chest | body | 2 | from the front | layered bark plates with a glowing heartwood tree-ring (glow) |
| back | body | 4 | from behind | a young tree growing from the back |
| pauld_r / pauld_l | arms | 3 / 5 | from the sides | burl pauldrons that sprout |
| bracer_r / bracer_l | arms | 6 / 8 | from the sides | root-fingered bracers |
| greave_r / greave_l | legs | 9 / 11 | from below | log greaves |
| aux | body | 12 | from below | four orbiting acorns (glow, detail) |

Sprites: leaves and motes, a ring sigil on the ground (additive). Style colour 0xFFC866.

Render passes: skin overlay (cutout, translucent while fading), pieces (cutout, glow additive), skin glow (additive), sprites (additive). Softening 0.85 of every blow while it runs (10 s); whoever strikes the wearer is pricked and rooted.

Preview cost: about 5552 vertices for the full set in the preview (advisory budget 6000).

## Summon and construct models

| Model | Prop | Size | Clips | What it is |
|---|---|---|---|---|
| tree_guardian | TREE_1 variant 0 | 27 bones, 62 cubes | idle, rise, slam | a treant of heartwood: trunk body, root feet, branch arms, a crown of leaves, glowing heart. Follows the caster and slams the nearest foe for 20 s; can be hit. |
| tree_lance | TREE_1 variant 1 | 20 bones, 25 cubes | idle, thrust | a lance of root that bursts out of the ground; used by Root Lance, 1.5 s each. |

GeoLite, box UV with an emissive `_glow` pass, checked by `tools/geo_check.py` and `GeoAssetsTest`. Painted by `client/prop/TreePropPainter` (`TreePropPainter` helpers). Summons are bound to the caster, can be hit and expire; the variant is the low byte of the prop param, the phase bits 8-9, the clock bits 10-15.

## Spells

Damage numbers are the base values given to the grimoire's `hurt` (scaled by the book, mastery and EP). Control follows `BalanceLaw.controlTicks` (bosses are capped, hard controls share the 12 second lock).

| Page id | Name | State | Numbers |
|---|---|---|---|
| attribute_bolt | Splinter Shot | KEPT id, remade | starter: bolt 9 damage, pierces, Slowness II 2.5 s |
| attribute_cut | Root Lance | KEPT id, remade | mid: 4 to 7 root lances in a line, one every 2 ticks |
| attribute_field | Grove | KEPT id, remade | zone: radius 5.5 x EP scale, 8 s, foes 3 damage per second and rooted, friends mend 2.5 percent per second |
| attribute_domain | Great Tree Rising | KEPT id, remade | signature: radius 6 x scale, 15 damage, thrown up, rooted (control capped by BalanceLaw), cooldown 45 s |
| attribute_guard | Bark Armor | KEPT id, remade | mid: Heartwood armour 10 s, +4 empower |
| heartwood_guardian | Heartwood Guardian | NEW | signature: treant summon 20 s, cooldown 60 s |
| seedling_volley | Seedling Volley | NEW | mid: five seeds, 5 damage each, rooted 2 s |
| timber_wall | Timber Wall | NEW | mid: 5 x 3 oak logs for 10 s, foes in front thrown back, 4 damage |
| verdant_canopy | Verdant Canopy | NEW | zone: radius 7 x scale, 10 s, friends mend 3 percent per second and regenerate, foes slowed and 2 damage per two seconds |
| heartwood_fist | Heartwood Charge | NEW | mid: dash 13 damage, throws back, Slowness II 2 s |

Lang: `tools/lang/tree_rebuild.json` (book description and a description for every page). Page names are literal strings in `book/TreeArts.java`.

## VFX

| Shape | Use | Look |
|---|---|---|
| TREE_FX1 | cast | Root Lance: a growth-ring sigil contracts on the caster, a lance of two twisted root straps flies to the target with bark shards trailing |
| TREE_FX2 | zone | Grove: a growth-ring sigil turns on the ground, branches grow round the rim one after another and put on leaves, leaves fall and sap rises |
| TREE_FX3 | impact | Great Tree Rising: a flash and a ground ring, a crown of root masses erupts upward and leans outward, bark and splinters fly, leaves fall |

`vfx/client/layer/TreeLayer.java` on the shared blocks of `TreeFx` (ground rings, standing shapes, strands, wind, debris, flash). Each shape stays inside the 400-vertex budget (80 to 184 used). Preview: `python tools/vfx_preview/preview.py` with the scenes in `tools/vfx_preview/scenes.d/tree_rebuild.json`; armour and summons: `python tools/render_preview/preview.py` with `tools/render_preview/scenes.d/tree_rebuild.json`.

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
