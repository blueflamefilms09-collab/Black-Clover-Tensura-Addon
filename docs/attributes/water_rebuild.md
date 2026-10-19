# Water Magic: the 1.93.0 rebuild

Water Magic (`MagicType.WATER`, book `book_water`, `WaterBook`) rebuilt after Noelle Silva: the Sea Dragon's spells and the Valkyrie Dress. This file follows `docs/rebuild/magic_rebuild_brief.md`
(armour, summon models, VFX, mechanics, docs). Nothing could be run in a game; every visual was checked with the repo's preview tools only (see the last section).

## NEW / REPLACED / KEPT

| What | Status | Notes |
|---|---|---|
| Temporary armour (`Aura.WATER`, `client/aura/WaterAura`) | NEW | the old Valkyrie Dress overlay (`ModeArmor.Mode.VALKYRIE`, `ModeArmorLayer`) stays registered and untouched but the Valkyrie Dress page no longer starts it (the new set replaces it on that page) |
| 16 armour piece models (`geo/entity/water_arm_*`) | NEW | tools/gen_water_aura_geo.py |
| Shared rig `client/aura/WaterAuraRig` | NEW | the one engine of the four armours (anchoring, slim arms, forge-in / shatter-out through `EquipAnimation`) |
| Summon models: Sea Dragon (`water_sea_dragon`), Aqua Javelin (`water_javelin`) | NEW | props `WATER_1`, `WATER_2`; the old `WATER_DRAGON` VFX shape stays registered but the pages no longer send it |
| VFX layer `WaterFxLayer` (`WATERFX_CAST / ZONE / BURST`) | NEW | `WaterMagicLayer`, `WaterSpellLayer` and every old shape stay registered |
| Pages `waterball`, `roar`, `cradle`, `aqua_javelin`, `sea_dragons_nest`, `valkyrie_dress` | KEPT (ids, order, names, cost tier, cooldowns) and REPLACED (implementation, in `book/WaterArts`) | numbers as before; `waterball` is still the Tensura water ball |
| Pages `sea_dragons_wrath`, `tide_lance`, `scale_mail`, `torrent` | NEW, appended after `valkyrie_dress` | 10 family pages of the 24 allowed |
| `WaterBook` static spell methods | KEPT, unused | not deleted |

## Canon

Sources: the Black Clover anime from memory. The wiki (blackclover.fandom.com) could NOT be reached from the build session (no network: `getaddrinfo ENOTFOUND`) and `.github/wiki/` has no page for Water
Magic, so nothing below was checked against the wiki text. User: Noelle Silva (Sea Dragon, the Valkyrie Dress / Valkyrie Armor, the sea dragon's roar, cradle and nest). The existing page names
(Sea Dragon's Roar, Cradle, Nest, Aqua Javelin, Valkyrie Dress) are the canon ones the mod already used. The four appended pages are honest extensions in the same naming style, NOT verified canon
names: Sea Dragon's Wrath (a volley of dragons), Valkyrie Tide Lance (the armour's lance), Sea Dragon's Scale Mail (a water shield), Sea Dragon's Torrent (a wave). The armour follows the Valkyrie Dress
(`docs/mode_armor_spec.md`: blue crystal plates, the avian water wings, the lance): crystal-blue scale plate with pearl trim, a fin crest, a flaring skirt of crystal plates, wings of water feathers.

## The temporary armour

Set of 16 models, each its own geo model in whole-body coordinates with an emissive `_glow` map, anchored on the player's own model part, forged in and shattered out by `EquipAnimation`
(delays 0 to 12 ticks, flying in from their own side, 85 to 100 percent growth over 8 ticks, shattering over the last 18; the numbers are the Djinn full equip's). Slim and wide arms (arm pieces narrowed to 80 percent about the arm's
pivot on the Alex model). Always translucent (crystal), lit up so it reads in the dark.

| Piece | Anchor | Delay | Cubes | Notes |
|---|---|---|---|---|
| chest | body | 1 | 10 | scale cuirass, pearl lames, the shell on the chest, collar, crystal spine on the back |
| skirt | body | 5 | 8 | seven crystal plates flaring from the belt, swaying (animated) |
| pauldron_r / _l | arm | 2 | 5 each | shell cap, pearl rim, two dragon fins standing up |
| vambrace_r / _l | arm | 4 | 4 each | scale bracer, pearl cuff, an outer fin |
| gauntlet_r / _l | arm | 5 | 5 each | pearl plate, webbed knuckle fins |
| greave_r / _l | leg | 6 / 7 | 5 each | scale thigh and shin, pearl knee, heel fin |
| helm | head | 0 | 10 | pearl circlet, three-fin crest, swept temple fins, brow gem (skipped for the local player in first person) |
| wing_r / _l | body | 8 | 7 each | six water feathers fanned from a pearl arm, beating (animated, faster in the air) |
| tail | body | 9 | 6 | four swaying segments and a fish fin (animated) |
| halo | head | 10 | 12 | a pearl ring with four droplets, turning (animated) |
| backdrop | body | 12 | 12 | pearl ring with a crystal trident behind the back, turning (animated) |

Passes (explicit render layers): 1 water skin shell (`translucent`, 1.04 x the body, head and hat layer hidden for this pass so the face stays the player's own) - 2 the pieces (`translucent`, glass) - 3 their glow
maps (additive, full bright) - 4 haze (additive, 1.08 x body) - 5 sprites: three ripples running out on the ground, the sea sigil, two streams winding up the body, drops falling off the hands, the sigil behind the back. About 5.9k vertices a frame
(preview budget note: keep under 6000); far players (> 24 blocks) skip the small pieces, > 64 blocks nothing. Style 0 = ordinary cast, style 1 = the Valkyrie Dress.

## The summon models (props)

| Model | Prop | Cubes / texture | Animation |
|---|---|---|---|
| `water_sea_dragon` | `WATER_1` | 48 cubes, 512 px + glow | `swim` (travelling wave through nine segments, jaw, fish tail), `idle` |
| `water_javelin` | `WATER_2` (variant 1 = the tide lance, 1.4 x) | 30 cubes, 256 px + glow | `fly` (rings turning round the shaft), `idle` |

Server: `prop/WaterProps` + the shared `prop/WaterPropFlight` (fly along the delta movement, face it, end with the life; param bits in `WaterPropMath`). Painter: `client/prop/WaterPropPainter` (translucent model, additive glow, sprites).
The props are the bodies; the damage stays in `WaterArts` (balance law, boss caps).

## VFX

`vfx/client/layer/WaterFxLayer`, textures `textures/particle/waterfx_*.png` (tools/gen_water_fx_textures.py: stream, ripple, drop, sigil, wave, spray, head). `WATERFX_CAST` (sigil and ripple charge, a head of spray running with three winding
strands round a straight stream, drops shed along it, a crest and a ripple where it lands), `WATERFX_ZONE` (sigil, three ripples running out, spouts curling up round the rim, falling drops), `WATERFX_BURST` (three ripples, eight wave
crests thrown out, a spray cloud, drops on arcs). At most 204 of the 400 vertices in the previews. Scenes: `tools/vfx_preview/scenes.d/water_fx.json`.

## Spells

| Id | Name | Tier | Does | Scales with EP / status |
|---|---|---|---|---|
| waterball | Sea Dragon's Waterball | starter | Tensura water ball (kept) | Tensura + `hurt` |
| roar | Sea Dragon's Roar | signature | the dragon flies to the target or aim point; 20 damage and a knock in 2.5 blocks | `GrimoireBook.size` for radius, balance law for damage |
| cradle | Sea Dragon's Cradle | zone | 8 s sphere: projectiles slowed, allies +2 HP / s (`BalanceLaw.heal` caps) | radius 4 x size |
| aqua_javelin | Aqua Javelin | mid | 26-block pierce, 12 damage | width x size |
| sea_dragons_nest | Sea Dragon's Nest | zone, cd 600 | 10 s dome: enemy projectiles removed, foes Slowness II and 3 / s, allies healed | radius 6 x size |
| valkyrie_dress | Valkyrie Dress | signature, cd 1200 | 30 s: the full armour (style 1), Speed II, Strength II, Resistance II, Jump II, Fire Resistance | fixed |
| sea_dragons_wrath | Sea Dragon's Wrath | signature, cd 900 | three dragons in a +-18 degree fan, 14 each, pierce, knock | size |
| tide_lance | Valkyrie Tide Lance | mid, cd 300 | 33-block lance, 22 damage through everything, knock | size |
| scale_mail | Sea Dragon's Scale Mail | mid, cd 900 | 15 s Absorption (amp 1 + EP modifier up to 3) and Resistance II, allies within 6 x size healed 3 | `EnergyBridge.modifier` |
| torrent | Sea Dragon's Torrent | zone, cd 400 | cone 9 x size: 10 damage, strong push, Slowness III 3 s (no hard control) | size |

Costs and cooldowns are the shared `BookPage` tiers. Every successful cast puts the Water armour on for 5 s (`WaterArmour.wrap`; a longer armour that is on is never shortened). Damage only through `GrimoireBook.hurt`;
casts run inside `SafeCast` as before.

## Files

New: `client/aura/WaterAura.java`, `client/aura/WaterAuraRig.java`, `prop/WaterProps.java`, `prop/WaterPropFlight.java`, `prop/WaterPropMath.java`, `prop/WaterRebuildSelfTests.java`, `client/prop/WaterPropPainter.java`,
`client/prop/WaterPropDraw.java`, `book/WaterArts.java`, `book/WaterArmour.java`, `book/WaterArmourRule.java`, `vfx/client/layer/WaterFxLayer.java`, `tools/water_aura_lib.py`, `tools/water_fx_lib.py`, `tools/gen_water_aura_geo.py`,
`tools/gen_water_summon_geo.py`, `tools/gen_water_fx_textures.py`, `tools/lang/water_rebuild.json`, preview scenes `tools/render_preview/scenes.d/water_*.json`, `tools/vfx_preview/scenes.d/water_fx.json`, test `src/test/.../book/WaterRebuildTest.java`.
Edited (additions only): `book/WaterBook.java` (the page list and one import), `aura/Aura.java`, `client/aura/AuraRegistry.java`, `prop/PropKind.java`, `prop/PropRegistry.java`, `client/prop/PropPainterRegistry.java`, `vfx/VfxShape.java`, `vfx/client/VfxManager.java`.

## Not verified

* The Black Clover wiki could not be read (no network): canon is from memory; the four new page names are extensions, not canon names.
* Nothing was run in a game. The preview tools (`tools/render_preview`, `tools/vfx_preview`) are re-implementations of the render path against stubs: the look, the equip animation, slim arms and the vertex budget were checked there; real lighting, translucency sorting against terrain and water, the shader-less additive look and frame cost are not.
* Balance (cooldowns, damage) is by analogy with the other books; not play-tested. The sound of every new effect is the old one (none added).
* The CI server smoke self-tests (`magic.water_rebuild` ...) are new and have never run in the real server.
* `WaterAuraRig` reads `PartPose.x / y / z` as fields (the real 1.21.1 class); the preview stub of `PartPose` was changed to match.
