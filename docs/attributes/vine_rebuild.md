# Vine Magic rebuild (1.93.0)

Magic type `MagicType.VINE`, grimoire book `book_vine`, painter `Aura.VINE`, props `PropKind.VINE_1`, VFX layer `VineLayer`. Self-tests `magic.tree_rebuild_pages`, `magic.tree_rebuild_assets`, `magic.tree_rebuild_props` (registered once for all six nature magics); JUnit `TreeRulesTest`.

## NEW / REPLACED / KEPT

| Part | State | Notes |
|---|---|---|
| Temporary armour (Thornbine armour) | NEW | No armour set existed for this magic. Added next to everything else. |
| Summon / construct models | NEW | vine_serpent, vine_whip, vine_wall |
| Page ids, order, tiers, mastery bits | KEPT | Every old id stays at its old position; new pages are appended last (`TreeRulesTest` checks this). |
| Texts and numbers of the five generic pages | REPLACED | The generic placeholder spells of the book are remade as Vine Magic spells; the ids stay. |
| Page list | NEW | Pages appended: vine_serpent, vine_wall, vine_swing, strangling_bloom, lash_barrage |
| VFX | NEW | Three new shapes VINE_FX1, VINE_FX2, VINE_FX3 (appended to `VfxShape`); the old `WIKI_MAGIC_*` shapes stay registered and are still used by the book for the cast circle. |
| Sprites | NEW | Eight coloured particle sprites `textures/particle/vine_*.png`. |
| Time Magic files, World Tree / Briar / Cherry Blossom / Fungus files | KEPT | Not touched. |

## Canon sources

- Black Clover: Vine Magic is the attribute of creating and controlling vines, thorns and tendrils. No wiki page of it was reachable; built from the name and what the magic is.
- Look: green braided vines, thorns, buds with a lime glow.
- The wiki was unreachable from the build sandbox. What is stated above as canon is only what was known without the page.

## Armour: Thornbine armour (`Aura.VINE`)

Eleven GeoLite pieces (`tools/gen_vine_rebuild_geo.py`); every piece grows in from below, as if the vines climbed the body from the ground. Delay 0 to 12 ticks per piece, forge-in over 8 ticks, shatter over the last 18 ticks, the same `EquipAnimation` as the Djinn Full Equip. First person hides the head pieces; the pieces drop detail at 24 and 56 blocks.

| Piece | Part | Delay (ticks) | Flies in | Model |
|---|---|---|---|---|
| helm | head | 0 | from below | braided circlet with tendril horns |
| crown | head | 12 | from below | ring of thorns and buds above the head (glow, detail) |
| chest | body | 2 | from below | criss-cross bands with a glowing bud (glow) |
| back | body | 4 | from below | four jointed vine whips arching over the shoulders |
| pauld_r / pauld_l | arms | 3 / 5 | from below | a fan of leaves on each shoulder |
| bracer_r / bracer_l | arms | 6 / 8 | from below | spiral bracers with groping tendril fingers |
| greave_r / greave_l | legs | 9 / 11 | from below | wrapped greaves |
| aux | body | 12 | from below | five thorn seeds spiralling round the body (detail) |

Sprites: leaves, lime motes, a thorn-ring sigil (additive). Style colour 0xB6FF5A.

Render passes: skin overlay (cutout, translucent while fading), pieces (cutout, glow additive), skin glow (additive), sprites (additive). Softening 0.90 of every blow while it runs (10 s); whoever strikes the wearer is rooted and pricked.

Preview cost: about 7044 vertices for the full set in the preview (advisory budget 6000), above the advisory budget.

## Summon and construct models

| Model | Prop | Size | Clips | What it is |
|---|---|---|---|---|
| vine_serpent | VINE_1 variant 0 | 18 bones, 43 cubes | idle, rise, strike | a jointed serpent of vine with a thorned head that bites, roots and poisons the nearest foe for 15 s; can be hit. |
| vine_whip | VINE_1 variant 1 | 19 bones, 29 cubes | idle, lash | a jointed whip of vine that rises from the ground and lashes; used by Lash Barrage (0.7 s each). |
| vine_wall | VINE_1 variant 2 | 13 bones, 61 cubes | idle | a wall of woven living vines with thorns; 10 s. |

GeoLite, box UV with an emissive `_glow` pass, checked by `tools/geo_check.py` and `GeoAssetsTest`. Painted by `client/prop/VinePropPainter` (`TreePropPainter` helpers). Summons are bound to the caster, can be hit and expire; the variant is the low byte of the prop param, the phase bits 8-9, the clock bits 10-15.

## Spells

Damage numbers are the base values given to the grimoire's `hurt` (scaled by the book, mastery and EP). Control follows `BalanceLaw.controlTicks` (bosses are capped, hard controls share the 12 second lock).

| Page id | Name | State | Numbers |
|---|---|---|---|
| attribute_bolt | Thorn Dart | KEPT id, remade | starter: bolt 9 damage, pierces, poison prick |
| attribute_cut | Vine Whip | KEPT id, remade | mid: line, 8 damage, hauls foes toward the caster |
| attribute_field | Thicket | KEPT id, remade | zone: 8 s, tangle, 2.5 damage per second, small magicule drain |
| attribute_domain | Verdant Maelstrom | KEPT id, remade | signature: 3 s of inward whirl, 6 damage per second plus a wound, cooldown 45 s |
| attribute_guard | Thorn Mantle | KEPT id, remade | mid: Thornbine armour 10 s |
| vine_serpent | Vine Serpent | NEW | signature: serpent summon 15 s, cooldown 50 s |
| vine_wall | Thicket Wall | NEW | mid: wall summon 10 s |
| vine_swing | Vine Swing | NEW | mid: hauls the caster to the aim point, 5 damage and rooted to foes at the landing |
| strangling_bloom | Strangling Bloom | NEW | mid: hard control on one foe (BalanceLaw.controlTicks, shares the 12 s lock), 5 damage then 2.5 per second, magicule drain, bleed |
| lash_barrage | Lash Barrage | NEW | zone: 3 s, whips at up to three foes, 4 damage each |

Lang: `tools/lang/vine_rebuild.json` (book description and a description for every page). Page names are literal strings in `book/VineArts.java`.

## VFX

| Shape | Use | Look |
|---|---|---|
| VINE_FX1 | cast | Thorn Whip: a spiral-tendril sigil contracts on the caster, two vine straps wind round the line like a cracking whip with thorns bristling off them |
| VINE_FX2 | zone | Thicket: the sigil turns on the ground, vine columns grow round the rim one after another and curl over at the top, thorns line the inner side |
| VINE_FX3 | impact | Verdant Maelstrom: a flash and a thorn star, the ground ring runs out, vines spiral up and lean inward over the centre, thorn shards and leaves fly |

`vfx/client/layer/VineLayer.java` on the shared blocks of `TreeFx` (ground rings, standing shapes, strands, wind, debris, flash). Each shape stays inside the 400-vertex budget (80 to 184 used). Preview: `python tools/vfx_preview/preview.py` with the scenes in `tools/vfx_preview/scenes.d/vine_rebuild.json`; armour and summons: `python tools/render_preview/preview.py` with `tools/render_preview/scenes.d/vine_rebuild.json`.

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

Vine Magic has no armour in the anime, so since 1.99.0 **no cast puts the temporary armour of this document on the caster** (policy kind NONE). Everything described above as the armour (the render layer, its painter, geo models, textures and generator) is KEPT on disk and registered in `AuraRegistry`, so old saves and tests still resolve; it is simply never started by a page. What the pages did besides the armour (damage cut, thorns, resistance, constructs, effects) is unchanged. Where the armour carried the magic's look, every cast of a family page now sends a small `VINE_FX2` glow that follows the caster instead (no armour piece is drawn). See `docs/rebuild/armour_policy.md` for the table and the canon reasons; the code is `book.ArmourPolicy` (data: `data/nusmp/armour_policy.json`).
