# Modification Magic: the 1.93.0 rebuild (`MODIFICATION`, Morris Libardirt)

Everything in this file was written without being able to run the game. What was type-checked, tested and looked at, and what was not, is in section 9.

## 1. NEW / REPLACED / KEPT

| Part | Status | Notes |
| --- | --- | --- |
| Temporary armour "the Dissector's Frame": `client/aura/ModificationAura`, `Aura.MODIFICATION` (appended), 11 geo pieces, vein shell skin | NEW | the generic guard page set `Aura.WIKI`; Modification now has its own painter (the WIKI painter and its 30 styles are untouched) |
| Constructs: `modification_tendril`, `modification_subject`, `modification_needle` (+ 11 armour models), props `MODIFICATION_1..3` (appended) | NEW | |
| VFX `VfxShape.MODIFICATION_FX1..3` (appended), `ModificationLayer`, 6 textures | NEW | the wiki-layer look (`WIKI_MAGIC_*` in 0xFFFF9B58) stays registered and still serves the Spade boss |
| Book: the five generic pages `attribute_bolt`, `attribute_cut`, `attribute_field`, `attribute_domain`, `attribute_guard` (ids, order, tiers, the domain's 900 cooldown) | KEPT ids | casts and names REPLACED (`book/ModificationBook`, `ModificationArts`); the generic `WikiBooks.attribute()` implementation stays for the other 29 attributes |
| Pages `specimen_thread`, `spell_rewrite`, `test_subjects`, `dissection_theatre`, `dissector_frame` | NEW | appended |
| `WikiBooks.attribute()` | EDITED (additive) | one line first: `if (magic == MODIFICATION) return ModificationBook.create();` |
| `ModificationRulesTest` (7 tests), self-test `magic.modification_rebuild` | NEW | |

## 2. Canon sources, honestly

* The wiki host could not be reached. The doc rests on `docs/arc/morris_libardirt.md` (the Spade boss doc of this repo; it too was written from search summaries) and on what the author knows: Morris Libardirt, a scholar of the Diamond Kingdom turned scientist of the Spade Kingdom's Dark Triad, uses Modification Magic, which changes the structure of physical and magical things; in the fight at the Spade castle he sent clawed tendrils to tear the Black Bulls' base apart, cancelled Vanessa's Fate Manipulation, went through Rouge's protection (tendrils act on intangible things), wanted to dissect the "Arcane Stage" mages, and was stopped by flowers from Grey and Gauche, Asta's anti-magic, Henry's brute force. His body re-forms against what hurt it (the boss doc's "Re-Structure" and "Spell Rewrite").
* Game rules, not canon: the ledger-based guard (`Re-Structure`), the thread, the theatre with two doors, the test subjects as edited creatures, the Frame's needle crown. They express the story, they are not in it. Anti-magic is not special-cased in this book.

## 3. The armour: the Dissector's Frame

| Piece | Anchor | Delay | Flies in from | What it is |
| --- | --- | --- | --- | --- |
| `modification_helm` | head | 0 | above | round tinted lenses on a thin frame (the face stays free), a bone skull-cap with an orange rune, a crown of three needles, a spinal neck guard |
| `modification_chest` | body | 2 | front | a ribcage of five bone ribs over a steel belly plate, a sternum, an orange core that pulses, a steel back plate with a vertebra spine, gorget |
| `modification_tasset` | body | 4 | below | belt of hanging scalpels and glass vials (sway) |
| `modification_pauldron_r/_l` | arms | 3 | outward | four stacked lames, a stack of vertebrae and a short tendril that sways |
| `modification_vambrace_r/_l` | arms | 5 | outward and front | bone sleeve with orange bands, THREE LONG CLAWS growing from the knuckles |
| `modification_greave_r/_l` | legs | 6 | below and front | bone-and-steel plates with an orange strip, a heel spur |
| `modification_back` | body | 8 | behind | FOUR CLAWED TENDRILS from the upper back, four segments and a two-hook claw each, arching back and outward, swaying on their own beats; clip `lash` exists |
| `modification_halo` | head | 12 | above | a ring of eight dissection needles turning over the head |

Passes: pieces solid `entityCutoutNoCull` / translucent while forging, with additive full-bright glow maps (orange runes); then the vein shell, the posed model 4 percent larger, additive (`RenderType.eyes`), `textures/aura/modification_veins.png`. Style 1 (five seconds, or Re-Structure's nine) leaves out the tendrils and the needle ring. The 9,700-vertex figure of the preview includes both glow passes; the head pieces are skipped in first person.

## 4. The constructs

| Model | Cubes | Clips | Used by |
| --- | --- | --- | --- |
| `modification_tendril` | 28 | idle, strike, emerge, wither | Clawed Tendrils and the Dissection Theatre's ring: a cracked steel mound, eight tapering segments, a claw of three hooks (about 2.6 blocks) |
| `modification_subject` | 44 | idle, walk, strike, rise, crumble | Test Subjects: a stitched human of two blocks, a bone-blade forearm, a clawed hand, exposed ribs, spine spikes, orange eyes |
| `modification_needle` | 6 | idle, spin | every needle of the volley, the Spell Rewrite and the Frame's crown: a long pin with a ring handle and an orange tip |

## 5. The spells

| Page id | Tier | Cooldown | What it does |
| --- | --- | --- | --- |
| `attribute_bolt` Dissection Needles | starter | 80 | 3 to 5 needles in a fan, 2 ticks apart (speed 1.8, 18 ticks), 6 damage and Weakness 3 s each, the first foe each meets |
| `attribute_cut` Clawed Tendrils | mid | 160 | tendrils under up to 2 to 4 foes within 16 blocks (the look target first), a strike after 14 ticks for 8; if the shared control lock is free, they are also dragged toward you (0.5, a third for bosses) |
| `attribute_field` Structural Unravel | zone | 300 | aimed point, radius 4.5 x size, 4 s: `Nullification.shatterBarriers` as it opens, then Slowness II, Weakness and 3 damage a second |
| `attribute_domain` Structure Shock | signature | 900 | radius 6.5 x size: 14 damage and a shove (1.2, 0.4 for bosses) on up to 12 foes; the 5 s Frame |
| `attribute_guard` Re-Structure | mid | 160 | 9 s: Resistance I, the 9 s Frame and the LEDGER: every blow that lands is noted by kind (melee, ranged, magic) with a half-life of 100 ticks; the kind that leads is cut by `adaptCut` (25 percent, +2 per EP step, at most 40); every other kind finds a hole (x1.1). Rotating the kind of attack beats repeating it (a unit test pins that). |
| `specimen_thread` Specimen Mark | mid | 360 | look target in 24 blocks, the shared control lock; 7 s: pulled in by 0.03 growing to 0.10 a tick (a walker makes 0.216; a boss feels a third), drained 1 percent of its magicule and cut for 2 every second, dissected (12 and Weakness 6 s) when it comes within 2.6 blocks |
| `spell_rewrite` | mid | 380 | 4 s: enemy shots within 4.5 blocks (at most 6) are discarded and thrown back at their shooters as needles for 5 |
| `test_subjects` | mid | 600 | two (three at modifier 3) subjects for 30 s (1.5x with EP at most): follow, strike with the blade arm every 18 ticks for 7 |
| `dissection_theatre` | signature | 1500 | a ring of tendrils (6 positions, two missing: the doors) closes round the aimed point from 8 to 3 blocks over 5 s; whoever touches the wall from inside outside the doors (every 15 ticks) takes 10 and is pushed in; at the end everything within 3.5 blocks takes 16 and Slowness III 2.5 s (1 s for bosses); the 5 s Frame |
| `dissector_frame` | signature | 900 | 20 s: the Frame, Resistance I, a share of every blow off (`adaptCut` x 0.8, at most 30 percent), and while it lasts one needle fires at the nearest foe within 12 blocks every `autoNeedleInterval` ticks (44, down to 24 with EP) for 4 |

Status helpers: `Nullification.shatterBarriers`, `EnergyBridge.drain`, `GrimoireBook.control` (thread) and `BalanceLaw.beginControl` (tendril drag), `AttributeEvents.incoming/playerTick/logout` (ledger, Frame upkeep; ledgers are dropped on logout). EP scaling: `GrimoireBook.hurt` (damage), `EnergyBridge.modifier` (needle, tendril, subject counts, the Frame's rate), `BalanceLaw.epScale` (cuts, summon life, subject size). A boss is held for a third and capped by the balance law.

## 6. VFX (`ModificationLayer`, `tools/gen_modification_vfx_textures.py`)

* FX1 needles: an edit ring opens at the hand; each needle is a long pin with its ring head trailing and a hot tip leading, a pale flare behind it; a cut flashes where the first lands.
* FX2 zone (power = radius, drawn 1.5x): the edit ring turns on the ground, a hexagonal lattice turns the other way over it, six clawed tendrils climb on the rim and sway, fragments rise and fall, a pulse ring every 28 ticks.
* FX3 impact: flash, ground shock ring, the lattice bursting open across the hint direction, six cuts out of the middle, ten fragments thrown out.
* Textures: `modification_ring` (256), `_needle` (32 x 256), `_tendril` (64 x 256), `_lattice` (256), `_slice` (256 x 32), `_shard` (64). Up to 90 vertices per instance.

## 7. Files

New: `book/ModificationBook.java`, `ModificationArts.java`, `ModificationRules.java`, `prop/ModificationProps.java`, `client/prop/ModificationPropPainter.java`, `client/aura/ModificationAura.java`, `vfx/client/layer/ModificationLayer.java`, `tools/gen_modification_geo.py`, `tools/gen_modification_vfx_textures.py`, `tools/lang/modification_rebuild.json`, scenes `tools/render_preview/scenes.d/modification_render.json` and `tools/vfx_preview/scenes.d/modification.json`, 14 geo models, `textures/aura/modification_veins.png`, 6 particle textures, `ModificationRulesTest`. The shared one-line edits are in `transmutation_rebuild.md` section 7; this magic's own: `WikiBooks.attribute()`.

## 8. Tests and previews

`geo_check` (14 models OK), `GeoAssetsTest`, `ModificationRulesTest`, self-test `magic.modification_rebuild` (page order, behaviours, models, and that the ledger adapts); type check clean. Previews: `python3 -B tools/render_preview/preview.py modification_tendril_idle modification_subject_idle modification_needle_idle modification_armour_forge modification_armour_full modification_armour_shatter` (with `EquipAnimation` in the class list, see `transmutation_rebuild.md`), `python3 -B tools/vfx_preview/preview.py modification_cast modification_zone modification_impact`.

## 9. NOT verified

* Nothing was run in the game; the previews are re-implementations.
* The ledger classifies a blow by its damage source (a projectile is ranged, `AntiMagic.isMagic` says magic, the rest melee): how Tensura's and other mods' sources fall into those three was not looked at in game.
* Needles and the Frame's auto-needles that miss are only a visual prop flying on; the theatre's tendrils walk inward by the data the spell stores in the prop's server-side data, which was not run. The theatre's doors are two fixed directions (0 and 180 degrees from the aimed centre), not floor-aware as the boss's are.
* The drag, the thread's pull and the shove were not felt; the numbers are the author's.
* The paper-thin parts of the Frame (needles, scalpels, claws) are 0.4 to 0.8 px boxes and may flicker at distance.

## 1.99.0 armour policy

Modification Magic has no armour in the anime, so since 1.99.0 **no cast puts the temporary armour of this document on the caster** (policy kind NONE). Everything described above as the armour (the render layer, its painter, geo models, textures and generator) is KEPT on disk and registered in `AuraRegistry`, so old saves and tests still resolve; it is simply never started by a page. What the pages did besides the armour (damage cut, thorns, resistance, constructs, effects) is unchanged. Where the armour carried the magic's look, every cast of a family page now sends a small `MODIFICATION_FX2` glow that follows the caster instead (no armour piece is drawn). See `docs/rebuild/armour_policy.md` for the table and the canon reasons; the code is `book.ArmourPolicy` (data: `data/nusmp/armour_policy.json`).
