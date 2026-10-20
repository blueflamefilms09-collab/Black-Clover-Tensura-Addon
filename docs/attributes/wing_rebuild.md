# Wing Magic rebuild (1.93.0)

Pipeline of `docs/rebuild/magic_rebuild_brief.md`. Key `wing`, `MagicType.WING`, colour `E6E2F4`, hit effect levitate, guard effect slow falling.

## NEW / REPLACED / KEPT

| Part | Status | What |
|---|---|---|
| Armour `Aura.WING`, `client/aura/WingAura` | NEW (Wing drew the shared `WikiAura` TRAILS blades, untouched for the others) | ten geo pieces, real articulated wings |
| Summons `PropKind.WING_1`, `WingProps`, `WingPropPainter` | NEW | feather hawk, feather blade |
| VFX `WING_FX1..3`, `WingLayer`, `textures/particle/wing_*` | REPLACED for Wing | |
| Pages 0-4 (`attribute_bolt`, `attribute_cut`, `attribute_field`, `attribute_domain`, `attribute_guard`) | KEPT ids / order / tiers / cooldown, behaviour REPLACED | |
| Pages 5-7 (`wing_dash`, `feather_storm`, `hawk_strike`) | NEW, appended | |

## Canon
The wiki is unreachable from the build environment and `.github/wiki` has no Wing page. The spells are the nearest honest version of Wing Magic (wings and feathers made into weapons and lift); no named user is asserted; names are generic.

## Armour (`tools/gen_wing_rebuild.py`, delays in ticks)
chest (body, 0, front): overlapping feather scales, gold yoke, winged emblem, glowing heart; helm (head, 2, above): circlet, feathered temple fins, crest plume; pauld_r / _l (arm, 3 / 4): three stacked feather plates; vamb_r / _l (arm, 5 / 6): vambrace with three swept feathers, gold gauntlet; belt (body, 6, below): gold belt, feather tassets; back (body, 8, from behind 18 px): the wings (bones wing_r / wing_l with outer_r / outer_l, ten flight feathers and coverts each, clip idle beat with the outer half lagging 0.12 of the cycle) and a gold halo; greave_r / _l (leg, 9 / 10): winged boots. Render types: pieces CUTOUT assembled / TRANSLUCENT while forging or shattering, glow maps ADDITIVE on chest and back only; drifting feathers TRANSLUCENT; halo plane behind the head, glints and a gold ground ring ADDITIVE. Slim arms narrowed to 88 percent. About 6300 vertices assembled.

## Summon models
`wing_hawk` (40 cubes; idle beat, dive: folded wings, body pitches with the entity), `wing_blade` (16 cubes; spin). `WING_1`: variant 0 hawk (circles the owner 2.5 blocks out and above, dives at the nearest hostile within 14 or the target a page gave it: 7 damage x EP power and a push, one dive per 1.5 s), 1 blade (orbits at shoulder height, flies at a hostile within 10 once a second: 5 damage x EP power, returns). Both fly (no gravity) and auto-target hostile mobs only.

## Spells
| # | id | Name | Tier | Effect |
|---|---|---|---|---|
| 0 | `attribute_bolt` | Feather Shot | starter | `ElementBook.volley` three feathers, 6 damage, levitation 1.25 s |
| 1 | `attribute_cut` | Wing Slash | mid | `ElementBook.line` 11 damage, 10 blocks, lift |
| 2 | `attribute_field` | Updraft | zone | r 4.5 x size, 6 s: levitation (control gate, `controlTicks`), 2 damage per second, allies slow fall |
| 3 | `attribute_domain` | Skyward | signature, 900 | +1.5 upward, slow fall 8 s, two hawks 20 s (up to +50 percent), 5 s armour |
| 4 | `attribute_guard` | Wing Armour | mid | 9 s: slow fall, no fall damage, speed I, jump II, empower +4 and thorns as before, armour set |
| 5 | `wing_dash` | Wing Dash | mid | `ElementBook.dash` 12 blocks, 7 damage, push |
| 6 | `feather_storm` | Feather Storm | zone | four blades 15 s |
| 7 | `hawk_strike` | Hawk Strike | mid | hawk dives at the looked-at non-ally, 90 ticks |

## VFX
FX1: charge wings unfolding at the hand, a great glowing feather with a streak, three fanned feathers, tumbling down feathers, feather burst at the end. FX2: gold halo rings on the ground, feathers climbing in a spiral, rim columns of rising light, glints. FX3: two great wings opening in the plane of the camera and folding, ring, scattered feathers, glints. Textures: wing_feather, wing_wing, wing_streak, wing_halo, wing_glint, wing_down. Previews: `vfx_preview wing_cast wing_zone wing_burst`; `render_preview wing_hawk_view wing_aura_armour wing_prop_hawk wing_prop_blade`.

## Not verified
Nothing was run in a game; previews are software re-implementations over stubs. The wiki could not be read. The wing beat amplitude, the hawk's pitch and the balance numbers are untested in play.

## 1.99.0 armour policy

Wing Magic has no armour in the anime, so since 1.99.0 **no cast puts the temporary armour of this document on the caster** (policy kind NONE). Everything described above as the armour (the render layer, its painter, geo models, textures and generator) is KEPT on disk and registered in `AuraRegistry`, so old saves and tests still resolve; it is simply never started by a page. What the pages did besides the armour (damage cut, thorns, resistance, constructs, effects) is unchanged. Where the armour carried the magic's look, every cast of a family page now sends a small `WING_FX2` glow that follows the caster instead (no armour piece is drawn). See `docs/rebuild/armour_policy.md` for the table and the canon reasons; the code is `book.ArmourPolicy` (data: `data/nusmp/armour_policy.json`).
