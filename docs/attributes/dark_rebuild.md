# Dark Magic: the 1.93.0 rebuild

Dark Magic (`MagicType.DARK`, book `book_dark`, `DarkBook`) rebuilt after Yami Sukehiro and his Dark Cloaked style. This file follows `docs/rebuild/magic_rebuild_brief.md`. Nothing could be run in a game; every visual was
checked with the repo's preview tools only (see the last section).

## NEW / REPLACED / KEPT

| What | Status | Notes |
|---|---|---|
| Temporary armour (`Aura.DARK`, `client/aura/DarkAura`) | NEW | Dark Magic had no player render layer before |
| 17 armour piece models (`geo/entity/dark_arm_*`) | NEW | tools/gen_dark_aura_geo.py; shared engine `client/aura/WaterAuraRig` (see water_rebuild.md) |
| Summon models: Black Blade, Crescent, Black Moon, Black Hole | NEW | props `DARK_1`, `DARK_2`; the old `DARK_*` / `DARK_SLASH_*` VFX shapes and `DarkMagicLayer`, `DarkSlashLayer` stay registered, the pages no longer send them |
| VFX layer `DarkFxLayer` (`DARKFX_CAST / ZONE / BURST`) | NEW | textures `darkrb_*` (the name `darkfx_*` was already taken by the old layer's textures, which are untouched) |
| Pages `avidya_slash` ... `dimension_slash_equinox` (nine) | KEPT (ids, order, names, tiers, cooldowns) and REPLACED (implementation, `book/DarkArts`) | numbers as in `CanonSpells`, which is untouched and still compiled |
| Pages `dark_cloak`, `shadow_embrace`, `black_hole_collapse` | NEW, appended | 12 family pages of the 24 allowed |
| `DarkBook.onDamageEntity` | NEW | the guard's lifesteal |

## Canon

From memory of the anime (the wiki could not be read, see the last section; `.github/wiki/` has no Dark Magic page). User: Yami Sukehiro (Captain of the Black Bulls): Dark Cloaked Avidya Slash, Avidya Wild Slash, Dimension
Slash (and Equinox), Black Hole, Black Moon (the Mana Zone: every enemy spell in the area is swallowed, his Death Thrust uses it), Black Blade, Iai Slash. These names were already in the mod. The armour is Yami's look turned into a temporary
armour: obsidian plate, a horned hood, a tattered cape, his two katanas crossed on the back, the black moon over the head and the black hole behind. The three appended pages are extensions in the Dark Cloaked naming style, not verified canon
names (a guard, a void, a bigger black hole).

## The temporary armour

17 models, whole-body coordinates, emissive glow maps, anchored on the player's model parts, forged in / shattered out by `EquipAnimation` (Djinn full-equip numbers). Slim and wide arms.

| Piece | Anchor | Delay | Cubes | Notes |
|---|---|---|---|---|
| chest | body | 1 | 9 | obsidian cuirass with a violet slit, high cloak collar |
| cape | body | 5 | 6 | five tattered strips swaying from a yoke (animated) |
| blades | body | 9 | 6 | two katanas crossed on the back |
| pauldron_r / _l | arm | 2 | 8 each | cap, plates, two spikes, a violet nose |
| vambrace_r / _l | arm | 4 | 4 each | |
| gauntlet_r / _l | arm | 5 | 6 each | four long claws |
| greave_r / _l | leg | 6 / 7 | 7 each | knee spike, violet cuff |
| helm | head | 0 | 11 | the hood (open at the face), brow band, two horns (skipped for the local player in first person) |
| wing_r / _l | body | 8 | 6 each | five long tattered shadow feathers, beating slowly |
| tendrils | body | 9 | 9 | three swaying chains of darkness at the waist |
| halo | head | 10 | 16 | the black moon ring, turning |
| backdrop | body | 12 | 10 | the black hole disc with a violet accretion ring, turning |

Passes: shadow skin shell (`translucent`, 1.04 x body, the face left alone), the pieces (`cutout`, `translucent` where the alpha is torn: cape, wings, tendrils, and while forging), glow maps (additive), violet haze (additive, 1.08 x body),
sprites: the black hole and the dark sigil on the ground, black smoke (alpha blended so it darkens), tendrils off the hands, the hole behind the back, the moon over the head. About 6.8k vertices a frame at 129 cubes (over the preview's 6000 note;
the small pieces drop out beyond 24 blocks). Style 1 = Equinox (the glow and the haze turn white-violet).

## The summon models (props)

| Model | Prop | Cubes / texture | Animation |
|---|---|---|---|
| `dark_black_blade` | `DARK_1` variant 0 (follows the caster), 2 (chopping) | 22 cubes, 512 px | `idle`, `slash` (flames of shadow flicker) |
| `dark_crescent` | `DARK_1` variant 1 (flies flat) | 22 cubes, 256 px | `idle` |
| `dark_black_moon` | `DARK_2` variant 0 | 26 cubes, 512 px | `idle` (core, ring and corona turning) |
| `dark_black_hole` | `DARK_2` variant 1 | 27 cubes, 512 px | `idle` (two accretion rings against each other) |

Server `prop/DarkProps` + `prop/WaterPropFlight`; painter `client/prop/DarkPropPainter`. A "dark beast" was not built: no canon creature of Yami's.

## VFX

`vfx/client/layer/DarkFxLayer`, textures `darkrb_*.png` (tools/gen_dark_fx_textures.py: slash, moon, hole, tendril, rune, wisp, crack). Darkness has to darken: black underlays are alpha blended (drawn first), violet edges additive.
`DARKFX_CAST` (rune and black bloom, a black underlay with three violet streaks, tendrils winding, ends in a crack and a rune ring), `DARKFX_ZONE` (black disc, accretion disc turning, rune ring, shadow pillars at the rim, motes spiralling in),
`DARKFX_BURST` (the moon rising with a corona, runes running out, three cracks of the void, shadow flung out). At most 232 of 400 vertices. Scenes: `tools/vfx_preview/scenes.d/dark_fx.json`.

## Spells

| Id | Name | Tier | Does | Scales / status |
|---|---|---|---|---|
| avidya_slash | Dark Cloaked Avidya Slash | starter | crescent, 17 blocks, 12 damage, cuts every spell in its path | balance law |
| black_hole | Black Hole | zone | 3 s, swallows spells within 5 blocks, Slowness VII and Weakness III on whoever cast them (Magic Jamming style) | |
| dimension_slash | Dark Cloaked Dimension Slash | signature, cd 300 | 18-block cut, 18 damage, spells destroyed | |
| death_thrust | Death Thrust | mid | 8 blocks / 13; under Black Moon 14 / 26 and the moon is spent | |
| black_blade | Dark Cloaked Black Blade | mid, cd 900 | +2 reach and Strength for 30 s; the black blade model hangs in front of you (30 s armour) | |
| avidya_wild_slash | Dark Cloaked Avidya Wild Slash | mid | six crescents sprayed round your look, 7 each | |
| black_moon | Dark Cloaked Black Moon | zone, cd 900 | 10 s: enemy spells within 12 blocks swallowed (allies' pass); the moon model hangs overhead | |
| iai_slash | Dark Cloaked Iai Slash | mid | dash through the nearest foe in 8 blocks, 15 (22 under Black Moon) | |
| dimension_slash_equinox | Dark Cloaked Dimension Slash: Equinox | signature | 48-block cut, 28 damage, three crescents | |
| dark_cloak | Dark Cloaked Guard | mid, cd 900 | 15 s Resistance II, Absorption (amp 1 + EP modifier), 12 percent of melee damage dealt comes back (capped 6 per hit, `BalanceLaw.heal`) | `EnergyBridge.modifier` |
| shadow_embrace | Dark Cloaked Shadow Embrace | zone, cd 600 | 4 s round you, radius 7 x size: Slowness III, Blindness, 3 damage a second, Energy Drain 2 percent per half second (`EnergyBridge.drain`, five foes at most) | size |
| black_hole_collapse | Black Hole Collapse | signature, cd 1200 | 3.5 s at the aim point, radius 8 x size: swallows spells; pulls foes in (hard control, only after `GrimoireBook.control`, never a boss), then 30 damage | size, `BalanceLaw.isBoss` |

Every successful cast puts the Dark armour on for 5 s; Black Blade, Black Moon, Equinox, Guard keep it for their duration (`WaterArmour`).

## Files

New: `client/aura/DarkAura.java`, `prop/DarkProps.java`, `client/prop/DarkPropPainter.java`, `book/DarkArts.java`, `vfx/client/layer/DarkFxLayer.java`, `tools/gen_dark_aura_geo.py`, `tools/gen_dark_summon_geo.py`,
`tools/gen_dark_fx_textures.py`, `tools/lang/dark_rebuild.json`, preview scenes `dark_*.json`. Shared with the other three: `client/aura/WaterAuraRig`, `prop/WaterPropFlight`, `book/WaterArmour`, `tools/water_aura_lib.py`, `tools/water_fx_lib.py`.
Edited (additions only): `book/DarkBook.java` (the page list and the lifesteal hook), and the shared registries listed in water_rebuild.md.

## Not verified

* Wiki not reachable; canon from memory; the three new pages are extensions.
* Nothing ran in a game; preview tools only (equip animation, forge / shatter, slim arms, budgets). Real translucency sorting of the black smoke and the additive violet on bright terrain are unseen.
* Balance not play-tested. Black Hole's per-tick spell removal, the guard's lifesteal and the collapse need a real server to confirm.
* Self-test `magic.dark_rebuild` never ran in a real server.
