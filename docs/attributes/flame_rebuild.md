# Flame Magic: the 1.93.0 rebuild

Flame Magic (`MagicType.FLAME`, book `book_fire`, `FireBook`) rebuilt after the Vermillion family (Mereoleona's Leo Rugiens and Calidos Brachium, Fuegoleon's Crimson Lion King and Flame Armour, Spiral Flame). This file follows
`docs/rebuild/magic_rebuild_brief.md`. Nothing could be run in a game; every visual was checked with the repo's preview tools only.

## NEW / REPLACED / KEPT

| What | Status | Notes |
|---|---|---|
| Temporary armour (`Aura.FLAME`, `client/aura/FlameAura`) | NEW | the old Salamander Spirit Dive overlay (`ModeArmor`) stays registered and is still started by the Spirit Dive page (which is not one of the family pages and was not touched) |
| 16 armour piece models (`geo/entity/flame_arm_*`) | NEW | tools/gen_flame_aura_geo.py; engine `WaterAuraRig` |
| Summon models: Flame Lion, Phoenix | NEW | props `FLAME_1`, `FLAME_2`; the old `FIRE_LION` shape stays registered, the page no longer sends it |
| VFX layer `FlameFxLayer` (`FLAMEFX_CAST / ZONE / BURST`) | NEW | `FlameMagicLayer`, `FireSpellLayer` and every old shape stay registered |
| Pages `exploding_fireball`, `sol_linea` | KEPT (still Tensura shots) plus the armour | |
| Pages `calderos`, `leo_rugiens`, `spiral_flame`, `wild_bursting_flame`, `ignis_columna`, `calidos_brachium` | KEPT (ids, order, names, tiers, cooldowns) and REPLACED (implementation, `book/FlameArts`) | numbers as in `FireBook` / `WikiSpells`, which are untouched |
| Pages `crimson_lion_king`, `flame_armour`, `phoenix_flare` | NEW, appended | 11 family pages of 24; Spirit Dive / Spirit Burst keep their numbers after the family pages |
| `FireBook.onDamageEntity` | NEW | Flame Armour's burning blows |

## Canon

From memory of the anime (wiki not reachable; `.github/wiki/` has no Flame page). Users: Mereoleona Vermillion (Leo Rugiens, Calidos Brachium), Fuegoleon Vermillion (the Crimson Lion King, the salamander and the flame armour), the
Vermillion house (Spiral Flame, Wild Bursting Flame, Ignis Columna, Calderos). Those page names were already in the mod. The armour is the "Crimson Lion" look (red and gold plate, a lion's helm, a mane of flame, the sun-lion disc). Flame Armour
and Phoenix Flare are honest approximations and extensions, not verified canon page names. A user named "Zora" was in the brief: Zora Ideale's magic is not Flame, nothing was built for him here.

## The temporary armour

16 models, whole-body coordinates, glow maps, anchored on model parts, `EquipAnimation` forge / shatter (Djinn numbers), slim and wide arms.

| Piece | Anchor | Delay | Cubes | Notes |
|---|---|---|---|---|
| chest | body | 1 | 9 | red breastplate, gold lames, the sun on the chest, gorget, ember vent on the back |
| skirt | body | 5 | 7 | swaying tassets |
| pauldron_r / _l | arm | 2 | 10 each | lion-head pauldrons: cap, gold plate, nose, fangs, two horns |
| vambrace_r / _l | arm | 4 | 4 each | |
| gauntlet_r / _l | arm | 5 | 6 each | four claws |
| greave_r / _l | leg | 6 / 7 | 7 each | knee spike, flame cuff |
| helm | head | 0 | 15 | lion crown helm: band, cap, crest ridge, ears, cheek guards, nose guard, brow fangs (skipped for the local player in first person) |
| wing_r / _l | body | 8 | 6 each | five flame feathers on a gold arm, beating |
| tail | body | 9 | 6 | lion's tail, a flame at the tip |
| halo | head | 10 | 8 | the ring of the crown, turning |
| backdrop | body | 12 | 10 | the sun-lion disc (gold ring and a lion's face plate), turning |

Flames that stand on the armour are camera-facing sprites in the local frame of the part they sit on and follow its forge: the three crest flames, nine mane flames round the head, eight crown flames on the ring, the shoulder horn flames.
Passes: ember skin shell (`translucent`, 1.04 x body, face left alone) - the pieces (`cutout`, translucent while forging) - glow maps (additive) - heat haze (additive, 1.08 x body) - sprites: the sun-lion sigil and the ring of fire on the ground, embers,
the sun-lion sigil turning behind the back, the flames. About 6.1k vertices a frame.

## The summon models (props)

| Model | Prop | Cubes / texture | Animation |
|---|---|---|---|
| `flame_lion` | `FLAME_1` | 43 cubes, 512 px | `run` (gallop pairs, jaw, tail wave, mane flicker; the lion stops after the run and plays `idle`), `idle` |
| `flame_phoenix` | `FLAME_2` | 34 cubes, 512 px | `fly` (wings of six flame feathers, tail fan, crest), `idle` |

Server `prop/FlameProps` + `prop/WaterPropFlight` (the lion's param carries its stop tick); painter `client/prop/FlamePropPainter` (flames licking up the lion's back, embers, fire on the ground under it; a trail of flames behind the phoenix).

## VFX

`vfx/client/layer/FlameFxLayer`, textures `flamefx_*.png` (tools/gen_flame_fx_textures.py: flame, tongue, ring, ember, lion, burst, lick, smoke). `FLAMEFX_CAST` (sun-lion sigil and bloom charge, a head of fire running with three winding strands round a
core, embers, a flash and a ring on arrival), `FLAMEFX_ZONE` (the sun-lion sigil turning, a ring of fire, flames standing round the rim, embers rising), `FLAMEFX_BURST` (starburst flash, rings, ten flames thrown out, embers on arcs, smoke).
At most 216 of 400 vertices. Scenes: `tools/vfx_preview/scenes.d/flame_fx.json`.

## Spells

| Id | Name | Tier | Does | Scales / status |
|---|---|---|---|---|
| exploding_fireball | Exploding Fireball | starter | Tensura fire ball (kept) | Tensura + `hurt` |
| sol_linea | Sol Linea | mid | Tensura fire lance (kept) | |
| calderos | Calderos | zone | five pillars in a cross at the aim point, 12 damage, lift, 5 s burning | size |
| leo_rugiens | Leo Rugiens | signature, cd 600 | the lion runs from you to the aim point (up to 20 blocks): 18 damage timed to when it reaches each foe, 12 where it lands | size |
| spiral_flame | Spiral Flame | mid | corkscrew 22 blocks, 13, burst 6 | size |
| wild_bursting_flame | Wild Bursting Flame | zone | three waves, 7 each and a push | radius 5.5 x size |
| ignis_columna | Ignis Columna | signature, cd 600 | column 2.5 s: 12 then 3.5 per half second, lifting | size |
| calidos_brachium | Calidos Brachium | signature, cd 600 | cone within 8 blocks, 20 damage, 8 s burning | |
| crimson_lion_king | Crimson Lion King | signature, cd 900 | the lion at 2.4 x runs 24 blocks: 26 on its way, explosion 16 in 5 x size blocks, 6 s burning | size |
| flame_armour | Flame Armour | mid, cd 900 | 30 s armour, Fire Resistance, Strength; every blow ignites for 4 s and adds `BalanceLaw.damage(4)` (`FireBook.onDamageEntity`) | balance law |
| phoenix_flare | Phoenix Flare | zone, cd 600 | phoenix 22 blocks: 12 damage and burning on its line, allies within 8 blocks healed 4, sunburst 8 in 4 x size blocks | size |

Every successful cast puts the Flame armour on for 5 s (`WaterArmour.wrap`). Damage only through `GrimoireBook.hurt`; no hard control.

## Files

New: `client/aura/FlameAura.java`, `prop/FlameProps.java`, `client/prop/FlamePropPainter.java`, `book/FlameArts.java`, `vfx/client/layer/FlameFxLayer.java`, `tools/gen_flame_aura_geo.py`, `tools/gen_flame_summon_geo.py`,
`tools/gen_flame_fx_textures.py`, `tools/lang/flame_rebuild.json` (keys `nusmp.page.book_fire.*`: the Flame book's key is `book_fire`), preview scenes `flame_*.json`. Shared: see water_rebuild.md.
Edited (additions only): `book/FireBook.java` (the page list, the import and the blow hook), shared registries.

## Not verified

* Wiki not reachable; canon from memory; Flame Armour and Phoenix Flare are extensions.
* Nothing ran in a game; preview tools only. The flames-as-sprites read pale in the preview (the additive white core); the real look against daylight is unseen.
* Balance not play-tested. The lion's hit timing copies the old formula (cube-root ease) with the new run length of 22 ticks.
* Self-test `magic.flame_rebuild` never ran in a real server.

## 1.99.0 armour policy

Flame Magic is policy kind FORM (Flame Armour). The armour is put on **only** by `flame_armour` (cast it to forge the armour in with the Djinn equip animation; cast it again to take it off, free of cost). It stays until it is toggled off, the mana runs out (a little mana is paid every second), the wearer is knocked out or dies, and shatters out with the same animation; while it is on it gives the stats of the policy table. A cast of any other page never puts it on and never resets it. The form is `spirit_dive`: Spirit Dive. It is held as long as the mana upkeep is paid; the spirit's ambient intake pays most of it; speed, attack and senses are raised while it lasts. See `docs/rebuild/armour_policy.md` for the table and the canon reasons; the code is `book.ArmourPolicy` (data: `data/nusmp/armour_policy.json`).
