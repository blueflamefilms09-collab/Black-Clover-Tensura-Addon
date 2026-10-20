# Transmutation Magic: the 1.93.0 rebuild (`TRANSMUTATION`, Grey the alchemist)

Everything in this file was written without being able to run the game. What was type-checked, tested and looked at, and what was not, is in section 9.

## 1. NEW / REPLACED / KEPT

| Part | Status | Notes |
| --- | --- | --- |
| Temporary armour "Alchemist's Gilded Plate": `client/aura/TransmutationAura`, `Aura.TRANSMUTATION` (appended), 11 geo pieces, glyph shell skin | NEW | the magic had no armour layer (a cast showed only the generic circle) |
| Geo constructs: `transmutation_spike`, `transmutation_sentinel`, `transmutation_blade` (+ 11 armour models) | NEW | `tools/gen_transmutation_geo.py` writes models, animations, textures and glow maps |
| Props `PropKind.TRANSMUTATION_1..3` (appended), `prop/TransmutationProps`, `client/prop/TransmutationPropPainter` | NEW | server behaviours + painters |
| VFX `VfxShape.TRANSMUTATION_FX1..3` (appended), `vfx/client/layer/TransmutationLayer`, 6 particle textures | NEW | the profile (`MagicShapes`) now finds the triple by name |
| `ALCHEMY_CIRCLE` shape and its layer (`ArcaneSpellLayer`) | KEPT | still registered, still used by other systems (Hephaestus, forms feedback, the Spade boss) |
| Pages `iron_spikes`, `magic_convert`, `quagmire`, `grand_transmutation` (ids, order, mastery bits) | KEPT | casts REPLACED by `book/TransmutationArts` (props, flowers, new VFX, short armour); the old `WikiSpells` versions stay in the code unused |
| Pages `alchemic_blades`, `flowering`, `transmuted_sentinel`, `alchemist_armour` | NEW | appended after the four old ones (`book/ext/TransmutationExt`) |
| `WikiBooks.transmutation()` | EDITED (additive) | the four casts point at `TransmutationArts`, ids come from `TransmutationRules`, `.plus(TransmutationExt.pages())` appended |
| Pure rules + `TransmutationRulesTest`, CI self-test `magic.transmutation_rebuild` | NEW | see section 8 |

## 2. Canon sources, honestly

* The wiki host (`blackclover.fandom.com`) could not be reached from the session (`getaddrinfo ENOTFOUND`); a web search returned no page for Transmutation Magic. What the doc rests on: the repo's own wiki text `.github/wiki/spell_ultra_giant_bull.txt` ("the Mirror Magic user multiplies the Transmutation Magic user who then converts a large structure nearby into the same material as the Recombination Magic user's magic house"), `docs/arc/morris_libardirt.md` (in the Spade raid Grey and Gauche turned Morris's tendrils into flowers), the mod's existing book (Grey: Iron Spikes, Magic Convert, Quagmire) and what the author knows of the series.
* NOT verified against the wiki: Grey's exact spell list. In the show Grey is better known for Transformation Magic (she changes into animals); the mod calls her attribute Transmutation Magic and the Ultra Giant Bull text confirms that name, so the pages stay as the mod had them and the new ones are built from the two canon facts above (substances converted, tendrils turned into flowers).

## 3. The armour: the Alchemist's Gilded Plate

Eleven geo pieces (humanoid proportions, each anchored on the model part it was built for, `EquipAnimation` forge-in and shatter-out, delays in ticks and fly-in side in model pixels):

| Piece | Anchor | Delay | Flies in from | What it is |
| --- | --- | --- | --- | --- |
| `transmutation_helm` | head | 0 | above (0,-14,0) | an open circlet: glyph band, gold front plate with a glass gem, swept-back fins (sway), neck guard, crest of three blades (face stays free; skipped in first person for the local player) |
| `transmutation_chest` | body | 2 | front (0,0,-14) | gorget, breastplate with the great circle (glass inlay, glowing rings), belly lames, back plate with a second circle, spine ridge, side straps |
| `transmutation_tasset` | body | 4 | below (0,10,0) | belt with a glass gem, hanging plates front, back and sides (sway) |
| `transmutation_pauldron_r/_l` | right / left arm | 3 | outward (-+14,-4,0) | four stacked lames and a gold cap, a transmutation circle standing on the shoulder that spins about its own axis |
| `transmutation_vambrace_r/_l` | right / left arm | 5 | outward and front (-+12,2,-6) | tapering sleeve, three gold bands, the alchemist's glove with a circle on the back of the hand, knuckles, a ring hovering at the elbow |
| `transmutation_greave_r/_l` | right / left leg | 6 | below and front (-+3,14,-6) | shin plate with glyphs, knee guard with a gem, sabaton |
| `transmutation_back` | body | 8 | behind (0,0,14) | THE GREAT CIRCLE: three discs turning against each other (hollow, cut out of the texture so only the rings show), six glyph stones orbiting, two rods |
| `transmutation_halo` | head | 12 | above (0,-16,0) | two counter-turning octagon rings with four tablets (skipped in first person) |

Arms are built around the wide arm (x -8..-4); on slim arms the pieces sit about a pixel proud, they are never inside the arm. Style 1 (the five-second armour after the Grand Transmutation) draws the same set without the back circle and the halo.

Passes (table of section 5 of the authoring guide): 1. each piece solid `entityCutoutNoCull` (`GeoDraw.Layer.CUTOUT`) once forged, `entityTranslucent` (`Layer.TRANSLUCENT`) while it forges in or shatters out, with its own additive glow map (`RenderType.eyes`, full bright); 2. the glyph shell: the posed player model again, 4.5 percent larger, additive (`RenderType.eyes`, full bright), `textures/aura/transmutation_glyphs.png`. Nothing is put in an inventory or armour slot. Budget: about 6,000 vertices for the whole set with every glow pass.

## 4. The constructs (geo models, glow maps, animations)

| Model | Cubes | Clips | Used by |
| --- | --- | --- | --- |
| `transmutation_spike` | 15 | rise (0.42 s), fall, idle | Iron Spikes (a rune disc in the ground, a main spike of five blocks, four leaning ones), 34 ticks each |
| `transmutation_sentinel` | 39 | idle, walk, strike, rise, crumble | Transmuted Sentinel (2 blocks of iron and stone, circle on the chest, ring behind the back, teal eyes) |
| `transmutation_blade` | 12 | idle, spin | Alchemic Blades (brass hilt, iron blade, glowing fuller, a ring under the guard); the painter turns it along its flight |

## 5. The spells (numbers are before the balance law: `GrimoireBook.hurt` multiplies by the EP curve, the cover and the mastery)

| Page id | Tier / cost | Cooldown | What it does |
| --- | --- | --- | --- |
| `iron_spikes` | starter | 80 | 9 spike props (5 to 16 with the spell size) every 2 ticks along your facing; each hits what stands within 1.3 blocks for 9 and gives Weakness 3 s |
| `magic_convert` | mid | 160 | 2 s circle in front: every enemy shot within 2.2 blocks is discarded in a burst of flowers, gives 4 percent of your magicule and Absorption 10 s |
| `quagmire` | zone | 300 | aimed ground, radius 4 x size, 5 s: Slowness IV, sinking, 2 damage a second |
| `grand_transmutation` | signature | 600 | blessings on foes in 8 blocks become curses (capped at 10 s and amplifier 2) and you take them; 10 damage; the 5 s armour |
| `alchemic_blades` | mid | 300 | 3 to 5 blades (EP modifier) circle you at 1.6 blocks for 10 s (1.5x with EP at most), lunge at the nearest hostile within 12 blocks, one cut of 5 x size each, return |
| `flowering` | mid | 260 | radius 7: allies heal 3 and lose 2 to 4 harmful effects, foes' shots and the other casters' constructs (props whose owner is not an ally, at most 6) become flowers, 14 temporary flowers for 10 s |
| `transmuted_sentinel` | mid | 400 | one sentinel for 30 s (1.5x with EP at most): follows, strikes every 24 ticks for 9, knocks back (a third as far for bosses) |
| `alchemist_armour` | signature | 900 | 20 s: the Gilded Plate, Resistance I, `armourCut` (20 percent, +1.5 percent per EP step, at most 35 percent) of every blow |

Status helpers used: `GrimoireBook.around` (never allies), `BalanceLaw.heal` (the shared heal cap), `BalanceLaw.damage` (props), `AttributeEvents.incoming` (the armour). Bosses: knock-back a third, no control effect is used by this book. EP scaling: `GrimoireBook.hurt` (damage), `EnergyBridge.modifier` (blade and flower counts), `BalanceLaw.epScale` (armour cut, summon life and size). Safe wrappers: the casts run inside `SafeCast`.

## 6. VFX (`TransmutationLayer`; textures by `tools/gen_transmutation_vfx_textures.py`)

* `TRANSMUTATION_FX1` cast: an alchemy circle opens at the hand and a smaller one turns against it in front; a bolt of matter from brass gold to teal, a four-pointed star on its head, sparkles shed along the path, a ring flash at the target.
* `TRANSMUTATION_FX2` zone (power = radius, the manager draws zones 1.5x): the great circle turns on the ground inside a pulsing ring with a gold circle turning against it, two ribbons of glyphs climb in hoops, iron spikes rise round the rim, glyph sparkles drift up.
* `TRANSMUTATION_FX3` impact: flash, ground shock ring, the circle stamped, a standing ring, eight spikes thrown out, six sparkles; the colour 0xFF9AD0 (Flowering) turns the spikes into falling petals and the palette rose.
* Textures: `transmutation_circle` (256), `_rune_band` (512 x 32), `_spike` (64 x 128), `_star` (64), `_petal` (64), `_ring` (256).

The Time Magic VFX is untouched. At most about 220 vertices per instance (budget 400).

## 7. Files

* New: `book/TransmutationArts.java`, `TransmutationRules.java`, `book/ext/TransmutationExt.java`, `prop/TransmutationProps.java`, `client/prop/TransmutationPropPainter.java`, `client/aura/TransmutationAura.java`, `vfx/client/layer/TransmutationLayer.java`, `tools/gen_transmutation_geo.py`, `tools/gen_transmutation_vfx_textures.py`, `tools/transmutation_geo_kit.py` and `tools/transmutation_vfx_kit.py` (the two kits are shared by the four rebuilds of this batch), `tools/lang/transmutation_rebuild.json`, scenes `tools/render_preview/scenes.d/transmutation_render.json` and `tools/vfx_preview/scenes.d/transmutation.json`, 14 geo models + animations + textures + glow maps, `textures/aura/transmutation_glyphs.png`, 6 particle textures, `src/test/.../book/TransmutationRulesTest.java`.
* Existing files edited (all additions): `aura/Aura.java` (`TRANSMUTATION`, `RECOMBINATION`, `MODIFICATION` appended), `client/aura/AuraRegistry.java`, `prop/PropKind.java` (9 constants appended), `prop/PropRegistry.java`, `client/prop/PropPainterRegistry.java`, `vfx/VfxShape.java` (9 shapes appended), `vfx/client/VfxManager.java` (3 layers), `book/WikiBooks.java` (see section 1).

## 8. Tests and how to look at it

* `python3 -B tools/geo_check.py transmutation_helm ...` (all 14 models OK) and `GeoAssetsTest` (4 pass in the local shim); `TransmutationRulesTest` (6 tests: old ids pinned in order, bounds of every number); CI self-test `magic.transmutation_rebuild` (the book's page ids in order, the three behaviours registered, the 14 models present as resources).
* Type check: no real error in the files; the negative control (a wrong `EnergyBridge` call) was caught.
* Previews (private builds, never in the repo): `python3 -B tools/render_preview/preview.py transmutation_sentinel_idle transmutation_armour_forge transmutation_armour_full transmutation_armour_shatter transmutation_spike_rise transmutation_blade_idle` (the geo scene must be in the same call; the aura scenes also need `client/aura/EquipAnimation.java` in the tool's class list, which the shipped `preview.py` does not have: add it to `REAL_BASE` or the aura scenes will not compile) and `python3 -B tools/vfx_preview/preview.py transmutation_cast transmutation_zone transmutation_impact`.
* In game: give yourself `nusmp:book_transmutation`, summon the grimoire, cast the pages in order; the armour pages are `alchemist_armour` (20 s) and the Grand Transmutation (5 s).

## 9. NOT verified

* Nothing was run in the game. Every image above is the preview tools' re-implementation of the renderer (a software rasteriser over stubs), not the game's: lighting, depth sorting against the real player skin, the real alpha test and slim arms were not seen.
* The sign of a yaw turn in the geo renderer was settled from the preview; the great circle and the halo rings were looked at, the pauldron circle spin axis only in a still.
* Props: movement, the sentinel's walking over steps, the blades' lunges and the flowers (`tempBlock`) were not run; the prop state word and the blade slot packing are unit-tested only through the pure rules.
* Numbers (damage, counts, cooldowns, the armour cut) are the author's choices; none was felt.
* The canon of Grey's spells (section 2).

## 1.99.0 armour policy

Transmutation Magic has no armour in the anime, so since 1.99.0 **no cast puts the temporary armour of this document on the caster** (policy kind NONE). Everything described above as the armour (the render layer, its painter, geo models, textures and generator) is KEPT on disk and registered in `AuraRegistry`, so old saves and tests still resolve; it is simply never started by a page. What the pages did besides the armour (damage cut, thorns, resistance, constructs, effects) is unchanged. Where the armour carried the magic's look, every cast of a family page now sends a small `TRANSMUTATION_FX2` glow that follows the caster instead (no armour piece is drawn). See `docs/rebuild/armour_policy.md` for the table and the canon reasons; the code is `book.ArmourPolicy` (data: `data/nusmp/armour_policy.json`).
