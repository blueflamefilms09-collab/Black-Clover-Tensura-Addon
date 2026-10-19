# Mirror Magic: the 1.93.0 rebuild

Magic: `MagicType.MIRROR`, book `book_mirror`, grimoire colour `0xDDEEFF` (spells' violet `0xB89CFF`), canon user Gauche Adlai. Part of the 1.93.0 "magic rebuild" (brief: `docs/rebuild/magic_rebuild_brief.md`).

## NEW / REPLACED / KEPT

| Part | Status | What |
|---|---|---|
| Temporary armour | NEW | `client/aura/MirrorAura` (`Aura.MIRROR`, appended to the enum): the **Glass Garb**, 8 GeoLite ghost pieces via `SpatialArmourKit` and `EquipAnimation`; the Real Doubles wear it too (`client/MirrorDoubleGarbLayer`, one `addLayer` line in `MirrorDoubleRenderer`) |
| Summon models | NEW | four standing mirrors `mirror_oval / round / arch / diamond` (`PropKind.MIRROR_1`) and the blade-edged mirror `mirror_blade` (`PropKind.MIRROR_2`), `client/prop/MirrorPropPainter`, `prop/MirrorProps` |
| Effects | NEW | `vfx/client/layer/MirrorFxLayer` for `MIRROR_FX1..3` (appended to `VfxShape`), 8 sprites from `tools/gen_mirror_textures.py` |
| Mirrors in the world | REPLACED | `MirrorWorks.mirror(...)` (every mirror a page puts in the world) now stands the `MIRROR_1` model instead of sending the flat `MIRROR_FRAME` effect; the shatter bursts use `MIRROR_FX3`, rays `MIRROR_FX1`; Mirrors Slash flies `MIRROR_2` blades, Mirrors Meteorite drops them, Full Reflection's great mirror stands before you and follows you (bit 16 of the param) |
| Pages | KEPT (ids, order, mastery bits, numbers) | `reflect_refrain`, `real_double`, `reflect_ray`, `large_reflect_ray`, `full_reflection`, `mirror_array`, `mirror_step`, `mirrors_slash`, `mirrors_meteorite` (the 0.42 kit, `MirrorWorks`: catching and returning shots, jams, doubles, travel) |
| Pages | NEW (appended through `Ext.join`) | `reflect_iris`, `glass_garb` |
| Old effects / textures | KEPT | `MirrorLayer` and `MIRROR_RAY / PANE / FRAME / ARRAY / SHATTER / STEP / DOUBLE`, `mirror_frame_*`, `mirror_glass_*`; `MIRROR_ARRAY` still draws the orbiting mirrors of Mirror Array; the entity `MirrorDoubleEntity` is untouched |

## Canon sources and honesty

Offline: no wiki access. `.github/wiki/spell_reflect_iris.txt` ("The Mirror Magic user creates an array of mirrors that reflect the appearance of the Eye Magic user. Any targets that look into the mirrors are caught in an enchantment, physically paralyzing them and blocking their magic. The array could then be used to strike the immobile targets with Reflect Refrain.") and the existing `MirrorWorks` notes (mirrors of every shape reflecting and weaponising light, the mirrored Real Double that casts with its original) are the canon used. **Reflect Iris** is the nearest honest version without an Eye Magic user. The **Glass Garb** is an original design (silver glass in violet-silver frames), not a verified canon outfit.

## Armour pieces (tools/gen_mirror_geo.py)

| Piece | Anchor | Content | Delay / fly-in |
|---|---|---|---|
| `mirror_chest` | body | frame cuirass, an ornate oval mirror breastplate in a gold frame, two shoulder panes at the collar, gems | 0 / front |
| `mirror_helm` | head | a crown of five tall mirror panes on a frame band, a brow mirror, shard ears | 2 / front-above |
| `mirror_arm_r` / `_l` | arms | a large angled pane on a pauldron cap, two framed panes on the forearm, framed gauntlet | 4 / 5 / sides |
| `mirror_leg_r` / `_l` | legs | framed shin pane, round knee mirror, framed boot | 7 / 8 |
| `mirror_back` | body | a fan of seven floating mirrors of different sizes swinging and bobbing (clip `idle`) | 10 / behind |
| `mirror_halo` | head | a tilted ring of eight shards circling the head (clip `idle`) | 12 |

Ghost pieces (always translucent: glass), glow map = frame edges, gems and a bright rim round every pane. Passes: pieces; ground ring (+ sigil in style 1), prism streaks fanned from the breast mirror (`mirror_reflex_prism`, alpha); travelling glints. About 170 cubes (about 8 000 vertices with glow).

## Summon models

* `mirror_oval / round / arch / diamond` (37 to 56 cubes): an ornate frame (ellipse segments turned to the normal, crest, gems, foot) with a translucent glass pane as stacked slabs and five shard chips drifting round it; clips `idle`, `open` (unfolds from a line). The prop's position is the middle of the glass; scale 1.33 x the half-height the old effect took.
* `mirror_blade` (39 cubes): a kite of glass with a white edge, a point and a grip; clips `idle`, `spin`. Param 0 flies along its yaw (0.9 blocks/tick), param 1 falls (1.0 block/tick, point down).

## Spells (power = `MirrorWorks.power` from EP and armour, 0.8 to 2.4; tiers 0 to 2)

Kept as they were: Reflect Refrain (a mirror holding 2 to 4 shots), Real Double (1 to 3 doubles, a tenth of max magicules each, 60 s), Reflect Ray (3 to 5 rays, 4.5 x power), Large Reflect Ray, Full Reflection (8 s, 60 percent returned), Mirror Array (3 to 5 mirrors, 40 s), Mirror Step (32 blocks), Mirrors Slash (9 x power, fragility), Mirrors Meteorite (6 to 10 mirrors, 8 x power each). New:

| Page | Numbers |
|---|---|
| `reflect_iris` (mid, 400 t) | up to 6 foes within 14 blocks that look at you (look vector dot > 0.35, line of sight): after the shared control gate, paralysis and silence for `BalanceLaw.controlTicks(t, 60)` (bosses shorter), 6 x scale; three mirrors round each |
| `glass_garb` (mid, 600 t) | 20 s: 30 percent of every shot or spell that hits you goes back to its sender (indirect magic), the rest x0.85; hook via `AttributeEvents.incoming` |

Lang: `tools/lang/mirror_rebuild.json`.

## Files

`tools/gen_mirror_geo.py`, `tools/gen_mirror_textures.py`, `tools/render_preview/scenes.d/mirror_*.json`, `tools/vfx_preview/scenes.d/mirror.json`;
`book/MirrorBook`, `MirrorRebuild`, `MirrorWorks` (edits listed above), `prop/MirrorProps`, `client/MirrorDoubleGarbLayer`, `client/MirrorDoubleRenderer` (one line); self-test `magic.mirror_rebuild`.

## Previews that were run

`preview.py mirror_oval_idle mirror_shapes mirror_blade_idle mirror_aura_armour mirror_aura_detail mirror_prop_oval mirror_prop_arch mirror_prop_diamond mirror_prop_blade` (the pose-stack check caught an unbalanced push in the first painter, fixed); `vfx_preview mirror_cast mirror_cast_big mirror_zone mirror_burst` (72, 112 and 96 of 400 vertices).

## NOT verified

* Nothing was run in a game. The glass transparency (alpha 105 of the pane texture on the translucent layer), the Real Doubles' garb on the mirrored (x = -0.9375) double renderer, and the follow behaviour of Full Reflection's mirror were not seen.
* In the geo preview the glass renders opaque (cutout), in the prop painter it is translucent: unconfirmed in game.
* Many mirrors can be alive at once (each reflected shot spawns a short-lived prop); the cap is the entity count, not tested.
* Canon: no wiki access; see above.
