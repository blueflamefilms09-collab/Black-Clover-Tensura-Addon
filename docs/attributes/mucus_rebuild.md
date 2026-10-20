# Mucus Magic rebuild (1.93.0)

Magic type `MagicType.MUCUS`, grimoire book `book_mucus`, painter `Aura.MUCUS`, props `PropKind.MUCUS_1`, VFX layer `MucusLayer`. Self-tests `magic.tree_rebuild_pages`, `magic.tree_rebuild_assets`, `magic.tree_rebuild_props` (registered once for all six nature magics); JUnit `TreeRulesTest`.

## NEW / REPLACED / KEPT

| Part | State | Notes |
|---|---|---|
| Temporary armour (Gelplate armour) | NEW | No armour set existed for this magic. Added next to everything else. |
| Summon / construct models | NEW | mucus_construct, mucus_tendril |
| Page ids, order, tiers, mastery bits | KEPT | Every old id stays at its old position; new pages are appended last (`TreeRulesTest` checks this). |
| Texts and numbers of the five generic pages | REPLACED | The generic placeholder spells of the book are remade as Mucus Magic spells; the ids stay. |
| Page list | NEW | Pages appended: mucus_construct, smother, absorption_wall, sticky_trail, mucus_burst |
| VFX | NEW | Three new shapes MUCUS_FX1, MUCUS_FX2, MUCUS_FX3 (appended to `VfxShape`); the old `WIKI_MAGIC_*` shapes stay registered and are still used by the book for the cast circle. |
| Sprites | NEW | Eight coloured particle sprites `textures/particle/mucus_*.png`. |
| Time Magic files, World Tree / Briar / Cherry Blossom / Fungus files | KEPT | Not touched. |

## Canon sources

- Black Clover: Mucus Magic smothers and entraps, absorbs physical attacks and absorbs magic power (the canon description known). Smother, Absorption Wall and the armour's soaking follow that description. The wiki page itself was not reachable.
- Look: translucent teal gel, glowing nuclei, bubbles, drips.
- The wiki was unreachable from the build sandbox. What is stated above as canon is only what was known without the page.

## Armour: Gelplate armour (`Aura.MUCUS`)

Eleven GeoLite pieces (`tools/gen_mucus_rebuild_geo.py`), ALL on the alpha-blended layer (the `translucent` style flag keeps a fully formed piece off the alpha-tested layer); they slide onto the body from the inside of each limb like a coat of slime. Delay 0 to 12 ticks per piece, forge-in over 8 ticks, shatter over the last 18 ticks, the same `EquipAnimation` as the Djinn Full Equip. First person hides the head pieces; the pieces drop detail at 24 and 56 blocks.

| Piece | Part | Delay (ticks) | Flies in | Model |
|---|---|---|---|---|
| helm | head | 0 | from above and the front | gel dome with a glowing nucleus and dripping strings (glow) |
| crown | head | 12 | from above | eight bubbles circling above the head (glow, detail) |
| chest | body | 2 | from the front | a swollen bladder with its nucleus visible through the layers (glow) |
| back | body | 4 | from behind | four waving pseudopod tendrils (glow) |
| pauld_r / pauld_l | arms | 3 / 5 | from the inside | stacked wobbling blobs on the shoulders (glow) |
| bracer_r / bracer_l | arms | 6 / 8 | from the inside | gel gloves |
| greave_r / greave_l | legs | 9 / 11 | from the inside | puddle boots |
| aux | body | 12 | from below | six orbiting bubbles (glow, detail) |

Sprites: rising bubbles, falling drips, a ring of gel on the ground (translucent / additive). Style colour 0x8CFFE0.

Render passes: skin overlay (cutout, translucent while fading), pieces (translucent), skin glow (additive), sprites (additive). Softening 0.65 of every blow while it runs (10 s), the softest hit of the six; blows feed the wearer a little and drain the magicules of whoever strikes.

Preview cost: about 5692 vertices for the full set in the preview (advisory budget 6000).

## Summon and construct models

| Model | Prop | Size | Clips | What it is |
|---|---|---|---|---|
| mucus_construct | MUCUS_1 variant 0 | 5 bones, 26 cubes | idle, rise, absorb | a mound of gel with a nucleus; the absorb clip plays when it swallows a projectile. Stands beside the caster 15 s, engulfs foes; can be hit. |
| mucus_tendril | MUCUS_1 variant 1 | 6 bones, 11 cubes | idle, strike | a gel tendril that lashes; used by Mucus Tendril (1 s). |

GeoLite, box UV with an emissive `_glow` pass, checked by `tools/geo_check.py` and `GeoAssetsTest`. Painted by `client/prop/MucusPropPainter` (`TreePropPainter` helpers). Summons are bound to the caster, can be hit and expire; the variant is the low byte of the prop param, the phase bits 8-9, the clock bits 10-15.

## Spells

Damage numbers are the base values given to the grimoire's `hurt` (scaled by the book, mastery and EP). Control follows `BalanceLaw.controlTicks` (bosses are capped, hard controls share the 12 second lock).

| Page id | Name | State | Numbers |
|---|---|---|---|
| attribute_bolt | Mucus Shot | KEPT id, remade | starter: bolt 9 damage, bursts, slows, takes a little magicule |
| attribute_cut | Mucus Tendril | KEPT id, remade | mid: line, 8 damage, slows, drags toward the caster |
| attribute_field | Sticky Field | KEPT id, remade | zone: 8 s, slowed hard, 2 damage per second, magicule absorbed |
| attribute_domain | Engulfing Tide | KEPT id, remade | signature: 13 damage, slowed, silenced, drained, cooldown 45 s |
| attribute_guard | Mucus Armor | KEPT id, remade | mid: Gelplate armour 10 s |
| mucus_construct | Mucus Construct | NEW | signature: construct summon 15 s, cooldown 50 s |
| smother | Smother | NEW | mid: hard control on one foe (shares the 12 s lock), 4 damage then 2 per second, drain |
| absorption_wall | Absorption Wall | NEW | mid: wall 8 s that dissolves bolts and drains foes beyond it |
| sticky_trail | Sticky Trail | NEW | mid: trail 6 s that slows, 2 damage per 1.5 s |
| mucus_burst | Bubble Burst | NEW | zone: bubbles drift and burst, 6 damage each, slowed |

Lang: `tools/lang/mucus_rebuild.json` (book description and a description for every page). Page names are literal strings in `book/MucusArts.java`.

## VFX

| Shape | Use | Look |
|---|---|---|
| MUCUS_FX1 | cast | Mucus Shot: a membrane sigil contracts on the caster while a bubble swells, a wobbling blob flies pulling a sticky strand and shedding bubbles |
| MUCUS_FX2 | zone | Sticky Field: the membrane mandala turns on the ground, ripples of gel run out, sticky strands rise and sway round the rim, bubbles float up |
| MUCUS_FX3 | impact | Engulfing Tide: a giant bubble swells round the centre with a glowing blob inside, strands arc across it, then it bursts in droplets and a ring of gel |

`vfx/client/layer/MucusLayer.java` on the shared blocks of `TreeFx` (ground rings, standing shapes, strands, wind, debris, flash). Each shape stays inside the 400-vertex budget (80 to 184 used). Preview: `python tools/vfx_preview/preview.py` with the scenes in `tools/vfx_preview/scenes.d/mucus_rebuild.json`; armour and summons: `python tools/render_preview/preview.py` with `tools/render_preview/scenes.d/mucus_rebuild.json`.

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

Mucus Magic has no armour in the anime, so since 1.99.0 **no cast puts the temporary armour of this document on the caster** (policy kind NONE). Everything described above as the armour (the render layer, its painter, geo models, textures and generator) is KEPT on disk and registered in `AuraRegistry`, so old saves and tests still resolve; it is simply never started by a page. What the pages did besides the armour (damage cut, thorns, resistance, constructs, effects) is unchanged. Where the armour carried the magic's look, every cast of a family page now sends a small `MUCUS_FX2` glow that follows the caster instead (no armour piece is drawn). See `docs/rebuild/armour_policy.md` for the table and the canon reasons; the code is `book.ArmourPolicy` (data: `data/nusmp/armour_policy.json`).
