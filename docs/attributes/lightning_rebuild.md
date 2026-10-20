# Lightning Magic: the 1.93.0 rebuild

Lightning Magic (`MagicType.LIGHTNING`, book `book_lightning`, `LightningBook`) rebuilt after Luck Voltia and his Raging Lightning / Thunder Fiend armour. This file follows `docs/rebuild/magic_rebuild_brief.md`. Nothing could be run in a game;
every visual was checked with the repo's preview tools only.

## NEW / REPLACED / KEPT

| What | Status | Notes |
|---|---|---|
| Temporary armour (`Aura.LIGHTNING`, `client/aura/LightningAura`) | NEW | the old Lightning God overlay (`ModeArmor.Mode.LIGHTNING_GOD`) stays registered; `WikiSpells.thunderFiend` / `blackLightningFiend` are untouched but the pages no longer call them |
| 16 armour piece models (`geo/entity/lightning_arm_*`) | NEW | tools/gen_lightning_aura_geo.py; engine `WaterAuraRig` |
| Summon models: Thunderbird, Thunder Lance | NEW | props `LIGHTNING_1`, `LIGHTNING_2` |
| VFX layer `LightningFxLayer` (`LIGHTNINGFX_CAST / ZONE / BURST`) | NEW | the old `LIGHTNING_*`, `FX_LIGHTNING_ARC` shapes stay registered |
| Pages `chain` ... `black_lightning_battle_fiend` (eight) | KEPT (ids, order, names, tiers, cooldowns) and REPLACED (implementation, `book/LightningArts`) | numbers as in `WikiSpells` / `LightningBook`; `thunder_lance` is no longer a Tensura shot (it is a bolt with the lance model), `thunderbird_cavalry` no longer `ElementBook.volley` |
| Pages `raging_lightning`, `thunder_drum`, `lightning_surge` | NEW, appended | 11 family pages of 24 |
| `LightningBook.onDamageEntity` (Fiend arcs, Thunder Boots charge) | KEPT unchanged | |

## Canon

From memory of the anime (wiki not reachable; `.github/wiki/` has no Lightning page). User: Luck Voltia (Black Bulls): Thunder Fiend, Black Lightning Battle Fiend (his armour: white-and-gold with bolts, `docs/mode_armor_spec.md`),
Pulsaranta, God of Lightning Rising Salim, Thunderbird Cavalry: Sky-Splitting Magic Bow Armament. These were already in the mod. "Raging Lightning" is named in the owner's brief; the page `raging_lightning` builds a 30 s armour with periodic
strikes (an honest approximation, not a verified spell description). `thunder_drum` (Raijin's drums, the Thunder God's drum ring) and `lightning_surge` are extensions, not verified canon names.

## The temporary armour

16 models, whole-body coordinates, glow maps, anchored on model parts, `EquipAnimation` forge / shatter (Djinn numbers), slim and wide arms.

| Piece | Anchor | Delay | Cubes | Notes |
|---|---|---|---|---|
| chest | body | 1 | 10 | white enamel, gold chevrons, bolt vent on the back |
| skirt | body | 5 | 5 | swaying tassets |
| pauldron_r / _l | arm | 2 | 5 each | gold plates, two swept spikes |
| vambrace_r / _l | arm | 4 | 4 each | |
| gauntlet_r / _l | arm | 5 | 6 each | gold finger plates |
| greave_r / _l | leg | 6 / 7 | 6 each | thunder boots with winged heels |
| helm | head | 0 | 17 | winged circlet, crown of bolts (skipped for the local player in first person) |
| wing_r / _l | body | 8 | 13 each | three zig-zag bolt wings, crackling (animated, fast) |
| tail | body | 9 | 5 | zig-zag tail |
| halo | head | 10 | 16 | eight electrodes, spinning fast |
| backdrop | body | 12 | 18 | Raijin's seven thunder drums on a gold ring, turning |

Passes: enamel shell (`translucent`, 1.04 x body, face left alone), pieces (`cutout`, translucent while forging), glow maps (additive, flickering), haze (additive, 1.08 x body), sprites re-rolled every three ticks: jagged arcs between points round
the body and from the palms to the ground, sparks, the electric ring and drum sigil on the ground, the sigil spinning behind the back. About 7.2k vertices a frame (139 cubes; over the preview note of 6000; small pieces drop beyond 24 blocks).
Style 0 = Raging Lightning (white enamel, electric blue); style 1 = Black Lightning (enamel tinted iron-grey, glow blood red), used by `black_lightning_battle_fiend`.

## The summon models (props)

| Model | Prop | Cubes / texture | Animation |
|---|---|---|---|
| `lightning_thunderbird` | `LIGHTNING_1` | 34 cubes, 512 px | `fly` (six bolt feathers a wing beating), `idle` |
| `lightning_lance` | `LIGHTNING_2` (variant 1 = black) | 23 cubes, 256 px | `fly` (the coil of electrodes turning), `idle` |

Server `prop/LightningProps` + `prop/WaterPropFlight`; painter `client/prop/LightningPropPainter` (arcs, sparks and flashes that re-roll every three ticks).

## VFX

`vfx/client/layer/LightningFxLayer`, textures `lightningfx_*.png` (tools/gen_lightning_fx_textures.py: bolt, arc, ring, flash, feather, sigil, spark). `LIGHTNINGFX_CAST` (ring and flash charge, a jagged bolt of ten legs with a wide sheath and a hot
core striking through, three forks, a flash, a ring and sparks where it lands), `LIGHTNINGFX_ZONE` (ring, drum sigil, bolts striking down at random places in turn), `LIGHTNINGFX_BURST` (flash star, shock rings, eight arcs across the ground, sparks).
At most 312 of 400 vertices. Scenes: `tools/vfx_preview/scenes.d/lightning_fx.json`.

## Spells

| Id | Name | Tier | Does | Scales / status |
|---|---|---|---|---|
| chain | Chain Lightning | starter | 9 to the target, 6 to two more within 5 blocks of the last | balance law |
| thunder_boots | Thunder God's Boots | mid | dash up to 6, next blow within 5 s carries the stored charge (LightningBook.onDamageEntity) | |
| thunder_lance | Thunder Lance | zone | a lance bolt, 13 damage, short push | size |
| thunder_fiend | Thunder Fiend | mid, cd 900 | 30 s: Speed III, Haste II, blows arc to a second foe; armour 30 s | |
| pulsaranta | Pulsaranta | mid | three homing balls, 7 each in 2 blocks | |
| rising_salim | God of Lightning Rising Salim | signature | pillar at the aim point: 22 in 4.5 x size blocks and a throw upward | size |
| thunderbird_cavalry | Thunderbird Cavalry ... | zone | eight thunderbirds in a fan (toward the target when there is one), 5 damage each where they strike | size |
| black_lightning_battle_fiend | Black Lightning Battle Fiend | signature, cd 1200 | 30 s: Speed IV, Strength II, Haste III, arcs to two more foes, black armour | |
| raging_lightning | Raging Lightning | mid, cd 900 | 30 s armour, Speed III, Strength II, a bolt strikes the nearest foe within 8 blocks every two seconds (5 damage) | balance law |
| thunder_drum | Raijin's Thunder Drums | zone, cd 600 | 5 s, radius 5 x size: three strikes every half second (6 each), 25 percent paralysis 1.5 s (`EnergyBridge.effect "paralysis"`, only after `GrimoireBook.control`, `BalanceLaw.controlTicks`, never a boss) | size |
| lightning_surge | Lightning Surge | mid, cd 200 | chain dash through up to four foes within 12 blocks, 9 each, stores the Boots charge | |

Every successful cast puts the Lightning armour on for 5 s (the Fiend pages and Raging Lightning for 30 s).

## Files

New: `client/aura/LightningAura.java`, `prop/LightningProps.java`, `client/prop/LightningPropPainter.java`, `book/LightningArts.java`, `vfx/client/layer/LightningFxLayer.java`, `tools/gen_lightning_aura_geo.py`,
`tools/gen_lightning_summon_geo.py`, `tools/gen_lightning_fx_textures.py`, `tools/lang/lightning_rebuild.json`, preview scenes `lightning_*.json`. Shared: see water_rebuild.md.
Edited (additions only): `book/LightningBook.java` (the page list and the import), shared registries.

## Not verified

* Wiki not reachable; canon from memory; Raging Lightning, the drums and the surge are extensions.
* Nothing ran in a game; preview tools only. The wing bolts read as sparse bars in the preview (a bolt wing as sprites might look better); the flicker rate and cost of the re-rolled arcs in a crowd are unmeasured.
* Balance not play-tested; `thunder_lance` and `thunderbird_cavalry` changed from Tensura / `ElementBook` projectiles to `SpellRuntime` bolts, so their interaction with Tensura's magic rules (barriers, resistances) is the shared `hurt` path only.
* Self-test `magic.lightning_rebuild` never ran in a real server.

## 1.99.0 armour policy

Lightning Magic is policy kind TOGGLE (Lightning Armour). The armour is put on **only** by `thunder_fiend`, `black_lightning_battle_fiend`, `raging_lightning` (cast it to forge the armour in with the Djinn equip animation; cast it again to take it off, free of cost). It stays until it is toggled off, the mana runs out (a little mana is paid every second), the wearer is knocked out or dies, and shatters out with the same animation; while it is on it gives the stats of the policy table. A cast of any other page never puts it on and never resets it. See `docs/rebuild/armour_policy.md` for the table and the canon reasons; the code is `book.ArmourPolicy` (data: `data/nusmp/armour_policy.json`).
