# Recombination Magic: the 1.93.0 rebuild (`RECOMBINATION`, Henry Legolant and the magic house)

Everything in this file was written without being able to run the game. What was type-checked, tested and looked at, and what was not, is in section 9.

## 1. NEW / REPLACED / KEPT

| Part | Status | Notes |
| --- | --- | --- |
| Temporary armour "the house worn as armour": `client/aura/RecombinationAura`, `Aura.RECOMBINATION` (appended), 11 geo pieces | NEW | no armour layer existed |
| The Raging Black Bull as a prop with a geo model (`recombination_bull`) | REPLACED | the page spawned a named iron golem; the golem code stays in `WikiSpells.ragingBlackBull` (unused) |
| Props `RECOMBINATION_1`, `RECOMBINATION_2` (existing ids) | REPLACED (look and behaviour) | were two floating plank boxes (`ModeledMagicProps.recombination`, kept in the code, no longer called); now a recombined ROOM and a ring of boards. `PropKind.RECOMBINATION_3` (the Bull) and `_4` (the drill) appended |
| VFX `VfxShape.RECOMBINATION_FX1..3` (appended), `RecombinationLayer`, 6 textures | NEW | `RECOMBINE_CONSTRUCT` and its layer (`ArcaneSpellLayer2`) KEPT registered |
| Pages `mana_corkscrew`, `bulwark`, `room_swap`, `raging_black_bull` (ids, order, mastery bits, the Bull's 2400 cooldown) | KEPT | casts REPLACED by `book/RecombinationArts`; old `WikiSpells` versions stay unused |
| Pages `house_armour`, `room_rebuild`, `ultra_giant_bull`, `plank_snare` | NEW | appended (`book/ext/RecombinationExt`) |
| `WikiBooks.recombination()` | EDITED (additive) | casts point at `RecombinationArts`, ids from `RecombinationRules`, `.plus(RecombinationExt.pages())` |
| `RecombinationRulesTest`, self-test `magic.recombination_rebuild` | NEW | |

## 2. Canon sources, honestly

* The wiki host could not be reached (`getaddrinfo ENOTFOUND`). A web search returned summaries (not the wiki pages): Henry rearranges the rooms and shape of the Black Bulls' base, turns the base into the Raging Black Bull (a giant robot) and, against Lucifero, uses absorbed magic and the transmuted castle pieces to make the Ultra Giant Bull; his curse drains others' magic. The repo's own wiki text `.github/wiki/spell_ultra_giant_bull.txt` gives the compound spell: a central cockpit for allies, numerous cannons on the body that fire beams of magic which can arc round corners, a very large cost.
* Not canon, chosen for the game: the Bull rams and fires volleys on its own; Bulwark's corner rooms; House Armour as a wearable (the story never shows the house worn); Plank Snare; Room Rebuild. "The cockpit" and "beams round corners" are NOT built (the Ultra form is a bigger Bull with four cannons at a time).

## 3. The armour: the house worn as armour

| Piece | Anchor | Delay | Flies in from | What it is |
| --- | --- | --- | --- | --- |
| `recombination_helm` | head | 0 | above | an open cap: four courses of roof tiles over an oak band, a chimney with a lamp, a lamp on the brow, the iron horns of the Black Bulls (sway) |
| `recombination_chest` | body | 2 | front | foundation stone at the belly, oak boards, a DOOR that swings and an iron handle, two lit windows with shutters, a roof-beam collar, crossed beams on the back |
| `recombination_tasset` | body | 4 | below | iron belt with a lamp, floorboards hanging as a skirt, front and back (sway) |
| `recombination_pauldron_r/_l` | arms | 3 | outward | gable-end shoulders: oak lame, three tile courses, an iron strap, a swinging lantern |
| `recombination_vambrace_r/_l` | arms | 5 | outward and front | half-log sleeve with two iron straps, a tile cuff, a lit slot, a stone gauntlet |
| `recombination_greave_r/_l` | legs | 6 | below and front | stilt boots: oak shin posts with iron brackets, a tile knee cap, a foundation block as the boot |
| `recombination_back` | body | 8 | behind | THE HOUSE: two storeys, five tile courses, a lit window pair, a door, a chimney with three smoke puffs rising on a 14 s loop, a small flag, THREE ROOMS circling it |
| `recombination_halo` | head | 12 | above | six loose planks circling the head |

Passes: pieces solid `entityCutoutNoCull`, translucent while forging or shattering, each with its additive full-bright glow map (windows, lamps); no shell pass. Style 1 (five seconds after the Raging Black Bull) leaves out the back house and the planks. Head pieces are skipped for the local player in first person.

## 4. The constructs

| Model | Cubes | Clips | Used by |
| --- | --- | --- | --- |
| `recombination_bull` | 66 | idle, walk, charge, fire, assemble, collapse | the Raging Black Bull: foundation-stone hooves, oak legs, a house for a body (tile roof, six lit windows, a door on the rump, a chimney), an iron-horned head with lit eyes, cannons on the sides |
| `recombination_room` | 17 | idle, rise, swap | Bulwark's corners, Room Swap's two ends, Room Rebuild |
| `recombination_drill` | 12 | idle | Mana Corkscrew: stacked turning rings of mana in a timber-and-iron casing |
| `recombination_halo` (reused) | | | the ring of boards (RECOMBINATION_2) at knee height |

## 5. The spells

| Page id | Tier | Cooldown | What it does |
| --- | --- | --- | --- |
| `mana_corkscrew` | starter | 80 | the drill prop and a piercing bolt (speed 1.7, 12 ticks, radius 0.9), 10 damage and a push |
| `bulwark` | mid | 160 | ring of dark oak planks for 10 s (temporary blocks), four rooms at the corners, Absorption II 10 s |
| `room_swap` | zone | 300 | swap with the look target (24 blocks), rooms rise at both places, target Slowness IV 1 s (half for a boss) |
| `raging_black_bull` | signature | 2400 | the Bull for 60 s (1.5x with EP at most), scale `bullScale` 1.1 to 1.34; follows, walks to hostiles within 22 blocks, RAMS (28-tick state: a 14-tick lunge, then a blow in a circle of 3.6 x scale round it: 14, knock-back 1.0, a third for bosses, at most 6 foes, every 50 ticks) and fires its CANNONS (2, three at modifier 3+, every 120 ticks, 6 each); the 5 s armour |
| `house_armour` | signature | 900 | 20 s: the armour, Resistance I, `armourCut` 25 percent (+1.6 per EP step, at most 38) of every blow |
| `room_rebuild` | mid | 400 | allies in 7 blocks (at most 8) heal `roomHeal` (6, +1.5 per EP step) and regenerate; a room rises at your side |
| `ultra_giant_bull` | signature | 4800 | the Bull must stand: it grows to 1.6x over 16 ticks for 20 s, fires 4 (5) cannons every 40 ticks for 9, rams for 20 |
| `plank_snare` | mid | 300 | look target in 22 blocks: the shared control lock (`GrimoireBook.control`), Slowness X and held still for `BalanceLaw.controlTicks(60)` (a boss for a quarter), then 4 damage |

Status helpers: `GrimoireBook.control`, `BalanceLaw.beginControl/controlTicks/heal/isBoss`, `AttributeEvents.incoming` (the armour). EP scaling: `GrimoireBook.hurt`, `BalanceLaw.damage` for the props' blows, `BalanceLaw.epScale` (Bull scale, life, armour cut, heal), `EnergyBridge.modifier` (cannon count).

The Bull's state word (synced in `param`): bits 0-1 state (1 ram, 2 fire), 2-17 start time mod 65536, bit 18 Ultra, 19-29 the Ultra start time mod 2048. The painters copy these constants; `RecombinationRulesTest` pins that the Ultra bit does not touch the other fields.

## 6. VFX (`RecombinationLayer`, `tools/gen_recombination_vfx_textures.py`)

* FX1 cast: a small blueprint opens at the hand; a drill of boards spirals along the path round a glowing core, nail sparks shed behind it.
* FX2 zone (power = radius, drawn 1.5x): the blueprint turns on the ground inside an octagon footprint ring turning the other way; twelve boards fly in one after the other and lock on the footprint; four posts rise; chimney smoke.
* FX3 impact: flash, the octagon ring as a shock wave, ten boards and nails thrown out and falling, dust puffs.
* Textures: `recombination_plank` (128 x 32), `_blueprint` (256), `_smoke` (128), `_post` (32 x 128), `_spark` (32), `_ring` (256). Up to 144 vertices per instance.

## 7. Files

New: `book/RecombinationArts.java`, `RecombinationRules.java`, `book/ext/RecombinationExt.java`, `prop/RecombinationProps.java` (REWRITTEN: the old two no-op behaviours), `client/prop/RecombinationPropPainter.java` (REWRITTEN), `client/aura/RecombinationAura.java`, `vfx/client/layer/RecombinationLayer.java`, `tools/gen_recombination_geo.py`, `tools/gen_recombination_vfx_textures.py`, `tools/lang/recombination_rebuild.json`, scenes `tools/render_preview/scenes.d/recombination_render.json` and `tools/vfx_preview/scenes.d/recombination.json`, 14 geo models, 6 particle textures, `RecombinationRulesTest`. Shared one-line edits are listed in `transmutation_rebuild.md` section 7 (one set serves the four rebuilds).

## 8. Tests and previews

`geo_check` (14 models OK), `GeoAssetsTest`, `RecombinationRulesTest` (7 tests), self-test `magic.recombination_rebuild`; type check clean (the negative control was caught). Previews: `python3 -B tools/render_preview/preview.py recombination_bull_idle recombination_bull_charge recombination_room_idle recombination_drill_idle recombination_armour_forge recombination_armour_full recombination_armour_shatter` (needs `EquipAnimation` in the tool's class list, see `transmutation_rebuild.md`), `python3 -B tools/vfx_preview/preview.py recombination_cast recombination_zone recombination_impact`.

## 9. NOT verified

* Nothing was run in the game; the previews are re-implementations (a software rasteriser over stubs).
* `ElementBook.vfx` still spawns a `RECOMBINATION_2` ring of boards (or a room for `RECOMBINE_CONSTRUCT`) for effects drawn through the book (the universal pages): their look changed with the painter, their number did not; not seen in game.
* The Bull's pathing (steps, walls, a roof above it), its ramming and cannon timing, and how big it feels at scale 1.1 to 1.9 were not run. The cockpit and the arcing beams of the Ultra Giant Bull are not built.
* The chimney smoke is blocky (cubes with a scale animation), not a soft particle.
* Bulwark's walls (`tempBlock`) and the rooms beside them may overlap on uneven ground.

## 1.99.0 armour policy

Recombination Magic has no armour in the anime, so since 1.99.0 **no cast puts the temporary armour of this document on the caster** (policy kind NONE). Everything described above as the armour (the render layer, its painter, geo models, textures and generator) is KEPT on disk and registered in `AuraRegistry`, so old saves and tests still resolve; it is simply never started by a page. What the pages did besides the armour (damage cut, thorns, resistance, constructs, effects) is unchanged. Where the armour carried the magic's look, every cast of a family page now sends a small `RECOMBINATION_FX2` glow that follows the caster instead (no armour piece is drawn). See `docs/rebuild/armour_policy.md` for the table and the canon reasons; the code is `book.ArmourPolicy` (data: `data/nusmp/armour_policy.json`).
