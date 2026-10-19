# Mud Magic rebuild (1.93.0)

Magic type `MagicType.MUD`, grimoire book `book_mud`, painter `Aura.MUD`, props `PropKind.MUD_1`, VFX layer `MudLayer`. Self-tests `magic.tree_rebuild_pages`, `magic.tree_rebuild_assets`, `magic.tree_rebuild_props` (registered once for all six nature magics); JUnit `TreeRulesTest`.

## NEW / REPLACED / KEPT

| Part | State | Notes |
|---|---|---|
| Temporary armour (Bogplate armour) | NEW | No armour set existed for this magic. Added next to everything else. |
| Summon / construct models | NEW | mud_golem, mud_spout |
| Page ids, order, tiers, mastery bits | KEPT | Every old id stays at its old position; new pages are appended last (`TreeRulesTest` checks this). |
| Texts and numbers of the five generic pages | REPLACED | The generic placeholder spells of the book are remade as Mud Magic spells; the ids stay. |
| Page list | NEW | Pages appended: mud_golem, mud_spout, mud_wall, mud_shackle, mud_barrage |
| VFX | NEW | Three new shapes MUD_FX1, MUD_FX2, MUD_FX3 (appended to `VfxShape`); the old `WIKI_MAGIC_*` shapes stay registered and are still used by the book for the cast circle. |
| Sprites | NEW | Eight coloured particle sprites `textures/particle/mud_*.png`. |
| Time Magic files, World Tree / Briar / Cherry Blossom / Fungus files | KEPT | Not touched. |

## Canon sources

- Black Clover: Mud Magic creates and controls mud. No wiki page of it was reachable; built from the name and what the magic is.
- Look: wet brown mud, amber cracks, thick clods, bubbling surface.
- The wiki was unreachable from the build sandbox. What is stated above as canon is only what was known without the page.

## Armour: Bogplate armour (`Aura.MUD`)

Eleven GeoLite pieces (`tools/gen_mud_rebuild_geo.py`) that drop onto the body like thrown clods (large, heavy upward offsets). Delay 0 to 12 ticks per piece, forge-in over 8 ticks, shatter over the last 18 ticks, the same `EquipAnimation` as the Djinn Full Equip. First person hides the head pieces; the pieces drop detail at 24 and 56 blocks.

| Piece | Part | Delay (ticks) | Flies in | Model |
|---|---|---|---|---|
| helm | head | 0 | from above | thick cowl with a visor slit and amber eyes |
| crown | head | 12 | from above | orbiting clods (glow, detail) |
| chest | body | 2 | from above | three cracked slabs with amber seams and a pectoral boulder (glow) |
| back | body | 4 | from above | a hunched hump with clod hands (glow) |
| pauld_r / pauld_l | arms | 3 / 5 | from above | big mounds on the shoulders |
| bracer_r / bracer_l | arms | 6 / 8 | from above | massive gauntlet fists |
| greave_r / greave_l | legs | 9 / 11 | from above | wading boots with a splash flare |
| aux | body | 12 | from above | four orbiting globs (glow, detail) |

Sprites: falling droplets, rising dust, a cracked-earth sigil under the feet. Style colour 0xFF9A3C.

Render passes: skin overlay (cutout, translucent while fading), pieces (cutout, glow additive), skin glow (additive), sprites (additive). Softening 0.70 of every blow while it runs (10 s), the hardest of the six; whoever strikes the wearer is slowed.

Preview cost: about 4344 vertices for the full set in the preview (advisory budget 6000).

## Summon and construct models

| Model | Prop | Size | Clips | What it is |
|---|---|---|---|---|
| mud_golem | MUD_1 variant 0 | 9 bones, 41 cubes | idle, rise, slam | a golem of mud with a boulder chest, amber cracks and clod fists. Follows the caster and pounds the nearest foe for 20 s; can be hit. |
| mud_spout | MUD_1 variant 1 | 14 bones, 30 cubes | idle, erupt | a spout of mud that bursts from the ground; used by Mud Spout, Mud Wall and Mud Shackle (1.5 s each). |

GeoLite, box UV with an emissive `_glow` pass, checked by `tools/geo_check.py` and `GeoAssetsTest`. Painted by `client/prop/MudPropPainter` (`TreePropPainter` helpers). Summons are bound to the caster, can be hit and expire; the variant is the low byte of the prop param, the phase bits 8-9, the clock bits 10-15.

## Spells

Damage numbers are the base values given to the grimoire's `hurt` (scaled by the book, mastery and EP). Control follows `BalanceLaw.controlTicks` (bosses are capped, hard controls share the 12 second lock).

| Page id | Name | State | Numbers |
|---|---|---|---|
| attribute_bolt | Mud Ball | KEPT id, remade | starter: bolt 9 damage, bursts, slows |
| attribute_cut | Mud Wave | KEPT id, remade | mid: line 10 wide, thrown back, slowed |
| attribute_field | Mud Pit | KEPT id, remade | zone: 8 s, sinks and slows hard, 2 damage per second, weakness |
| attribute_domain | Bog Domain | KEPT id, remade | signature: 15 damage, thrown back, then mired 6 s, cooldown 45 s |
| attribute_guard | Mud Armor | KEPT id, remade | mid: Bogplate armour 10 s |
| mud_golem | Mud Golem | NEW | signature: golem summon 20 s, cooldown 60 s |
| mud_spout | Mud Spout | NEW | mid: spouts under up to four foes, damage, thrown up |
| mud_wall | Mud Wall | NEW | mid: wall of packed mud, foes in front thrown back, 4 damage |
| mud_shackle | Mud Shackle | NEW | mid: hard control on one foe (shares the 12 s lock), 4 damage |
| mud_barrage | Mud Barrage | NEW | zone: 3 s of globs on the aim point, 4 damage each |

Lang: `tools/lang/mud_rebuild.json` (book description and a description for every page). Page names are literal strings in `book/MudArts.java`.

## VFX

| Shape | Use | Look |
|---|---|---|
| MUD_FX1 | cast | Mud Ball: amber fissures spread on the ground sigil while mud gathers, a heavy glob flies on a sagging line of drops and dust |
| MUD_FX2 | zone | Mud Pit: the cracked sigil sinks into the ground, ripples run out, blobs of mud rise, swell and burst, clods sink, dust drifts |
| MUD_FX3 | impact | Mud Eruption: a flash, a column of mud shoots up and crowns in a splash, clods fly, a ring of dust runs out, the ground splits with amber light |

`vfx/client/layer/MudLayer.java` on the shared blocks of `TreeFx` (ground rings, standing shapes, strands, wind, debris, flash). Each shape stays inside the 400-vertex budget (80 to 184 used). Preview: `python tools/vfx_preview/preview.py` with the scenes in `tools/vfx_preview/scenes.d/mud_rebuild.json`; armour and summons: `python tools/render_preview/preview.py` with `tools/render_preview/scenes.d/mud_rebuild.json`.

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
