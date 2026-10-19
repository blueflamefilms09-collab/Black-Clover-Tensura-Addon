# Permeation Magic: the 1.93.0 rebuild

Magic: `MagicType.PERMEATION` (a wiki attribute, `WikiBooks.attribute`), book `book_permeation`, colour `0x65D7E8`. Part of the 1.93.0 "magic rebuild" (brief: `docs/rebuild/magic_rebuild_brief.md`).

## NEW / REPLACED / KEPT

| Part | Status | What |
|---|---|---|
| Temporary armour | NEW | `client/aura/PermeationAura` (`Aura.PERMEATION`, appended to the enum): the **Phase Shroud**, 8 GeoLite ghost pieces via `SpatialArmourKit` and `EquipAnimation` |
| Summon models | NEW | `permeation_echo` (phase echo, `PropKind.PERMEATION_1`) and `permeation_door` (phase door, `PropKind.PERMEATION_2`), `client/prop/PermeationPropPainter`, `prop/PermeationProps` |
| Effects | NEW | `vfx/client/layer/PermeationFxLayer` for `PERMEATION_FX1..3` (appended to `VfxShape`), 8 sprites from `tools/gen_permeation_textures.py` |
| Pages | KEPT (ids, order, behaviour) | the five generic wiki pages `attribute_bolt`, `attribute_cut`, `attribute_field`, `attribute_domain`, `attribute_guard`: they are built in the shared `WikiBooks` / `ElementBook`, which this rebuild does not edit, so they stay generic (honest limitation) |
| Pages | NEW (appended) | `phase_step`, `phase_through`, `permeating_strike`, `ghost_zone`: `book/ext/PermeationExt` through one added line in `ExclusiveExt` (`case PERMEATION`), code in `book/PermeationWorks` |
| Armour trigger | NEW | every new page forges the Phase Shroud for 5 s; the generic guard page still sets the shared wiki armour (`Aura.WIKI`, style 8), and a `playerTick` hook in `PermeationWorks` turns it into `Aura.PERMEATION` for its 180 ticks (one-tick flash of the wiki set on the client is possible) |
| Old effects | KEPT | the wiki layers (`WikiMagicLayer`, `WikiAura` for the 29 other wiki magics) and the `wiki_permeation` texture |

## Canon sources and honesty

Offline: no wiki access, and the repo has no Permeation wiki file. What is used is the general description of Permeation Magic (pass through matter) and nothing else; **no named canon user or spell list could be verified**. The four pages are the nearest honest spells to "passing through", clearly adaptations; the Phase Shroud, echo and door are original designs.

## Armour pieces (tools/gen_permeation_geo.py), all ghost (translucent)

| Piece | Anchor | Content | Delay / fly-in |
|---|---|---|---|
| `permeation_chest` | body | ribcage lattice: six upright bars, three rungs, spine ridge, collar and waist rails, a core gem, a faint glass skin | 0 / from behind (inside) |
| `permeation_helm` | head | three thin rings over the brow (face free), side bars, a hood of five wisps streaming back | 2 |
| `permeation_arm_r` / `_l` | arms | hollow pauldron frame, two open rings round the forearm, ghost glove of thin plates | 4 / 5 |
| `permeation_leg_r` / `_l` | legs | ladder of two bars and three rungs up the shin, rings at ankle and knee | 7 / 8 |
| `permeation_back` | body | the phase cloak: seven wisp slabs swaying, two lattice wings of six thin feathers each (clip `idle`) | 10 |
| `permeation_halo` | head | a gyroscope of three tilted rings with core beads, turning (clip `idle`) | 12 |

The pieces phase IN from inside the body with short inward vectors (`EquipAnimation`, delays 0 to 12) and out over the last 18 ticks. Passes: ghost pieces + glow; ground ring and an outgoing ripple; scanner rings rising through the body and bubbles drifting up (additive). About 200 cubes (about 9 850 vertices with glow: above the preview's soft budget note).

## Summon models

* `permeation_echo` (82 cubes): the hollow outline of a humanoid (bars on every edge of head, torso, arms and legs), faint glass inside, two core gems, a tail of wisps; clip `idle`. Appears at 70 percent in 3 ticks and fades over its life.
* `permeation_door` (33 cubes): a 28 x 48 px doorway of thin bars with rune ticks, a membrane of 12 hanging wisp slabs and six drifting lattice chips; clips `open`, `idle`, `close`. The `permeation_door` texture and an expanding ripple are laid in its plane (additive, both sides).

## Spells (EP scale = `EnergyBridge.scale`)

| Page | Numbers |
|---|---|
| `phase_step` (mid, 240 t) | 3 s: physical blows and shots (projectile damage, or a living direct attacker) are cancelled, spells take half (hook via `AttributeEvents.incoming`, never against `BYPASSES_INVULNERABILITY`); speed II; an echo stays |
| `phase_through` (mid, 120 t) | wall within 3 blocks in the look direction, destination = first free space behind it within 8 blocks (a bounding-box walk with `noCollision`), two phase doors, fall distance reset; fails with no wall or no room |
| `permeating_strike` (mid) | target within 6: 15 x scale, 90 percent armour bypass, barriers within 3 cut down, an echo goes through to the far side |
| `ghost_zone` (zone, 600 t) | 8 s, radius 5 x grimoire size: you and allies take 40 percent less from everything (flag refreshed every 10 ticks), foes slowed I |

Damage via `GrimoireBook.hurt` (balance law). Lang: `tools/lang/permeation_rebuild.json`.

## Files

`tools/gen_permeation_geo.py`, `tools/gen_permeation_textures.py`, `tools/render_preview/scenes.d/permeation_*.json`, `tools/vfx_preview/scenes.d/permeation.json`;
`book/PermeationWorks`, `book/ext/PermeationExt`, `book/ext/ExclusiveExt` (one case line), `prop/PermeationProps`; self-test `magic.permeation_rebuild`.

## Previews that were run

`preview.py permeation_echo_idle permeation_door_idle permeation_aura_armour permeation_aura_detail permeation_prop_echo permeation_prop_door`; `vfx_preview permeation_cast permeation_cast_big permeation_zone permeation_burst` (72, 104 and 68 of 400 vertices).

## NOT verified

* Nothing was run in a game; the previews are re-implementations. The see-through sets depend on translucent sorting in the real client.
* `phase_through` was type-checked only: a wall at the world edge, in an unloaded chunk or with a ceiling above are untested cases; it does not tunnel through bedrock-thick walls (8 block limit).
* The generic wiki pages stay generic; their visuals still come from the shared wiki layers.
* Canon: no wiki access and no verifiable user or spells; see above.
