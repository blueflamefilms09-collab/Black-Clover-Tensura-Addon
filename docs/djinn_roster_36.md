# The 36 Djinn (1.93.0): twelve buffed, twenty-four new

The roster is `relics/djinn/Djinn.java` (one row per Djinn), the weapons are `relics/djinn/weapon/DjinnWeaponRules.java` and `ActLibrary.java`. Every number below is read from those tables by a tool, not typed. `docs/djinn_roster_36.json` has the same data in a form the asset tools can read (weapon names, act keys, names and texts).

Enum entries are appended only: the first twelve keep their ordinals (the sync packet writes `ordinal + 1` as a byte), the twenty-four follow Vassago in the owner's order. Each new Djinn has a seal (`djinn_seal_<id>`), a light and a dark weapon (`djinn_weapon_<id>`, `djinn_weapon_<id>_dark`), a trait, an Extreme Magic and a look of its own.

## 1. The table

| # | Djinn | Affinity | Colours | Look (horns, wings, tail, halo, ears) | Trait (Half Equip) | Extreme Magic |
|---|---|---|---|---|---|---|
| 1 | Baal, Lord of Thunder | Lightning | 0x3C8CFF / 0xBFE4FF | crescent blade whip spokes ears | Chain: +30% of the blow, at most 7.5; slowness 3s | Baal Bolg: Judgement of Thunder: bolts, reach 15, damage 30, slowness 4.5s |
| 2 | Amon, Sovereign of Flame | Heat | 0xFF5A14 / 0xFFD27A | ram fire flame ring no ears | Melt: +35% of the blow, at most 8; weakness 4.5s | Amon Salos: Crown of the Sun: nova, reach 13, damage 32, burn 8s |
| 3 | Vinea, Queen of the Deep | Water | 0x2D9CDB / 0xB5F0FF | crescent veil tendril runes ears | Freeze: +25% of the blow, at most 6.5; slowness 4.5s | Vinea Zalg: The Drowning Sea: wave, reach 16, damage 25, slowness 6s |
| 4 | Focalor, Warden of Gales | Wind | 0x62D6A0 / 0xE6FFF1 | spike feather whip ring ears | Push: +30% of the blow, at most 7.5; no effect | Focalor Zaleph: Seven-Wind Cyclone: vortex, reach 15, damage 23, nausea 7.5s |
| 5 | Zagan, Keeper of Life | Life | 0x58C75A / 0xE4FFB8 | stag feather tendril runes ears | Lifesteal: +25% of the blow, at most 6.5; no effect | Zagan Genesis: Bloom of Life: heal_field, reach 14, damage 18, weakness 7.5s |
| 6 | Zepar, Breaker of Giants | Strength | 0xD43A3A / 0xFFB9A0 | ram membrane blade none no ears | Crush: +35% of the blow, at most 8; weakness 4.5s | Zepar Fang: Titan's Fall: nova, reach 11, damage 34, slowness 3s |
| 7 | Furfur, Voice of Ruin | Sound | 0xB77BFF / 0xF1DDFF | crown veil tendril runes ears | Deafen: +25% of the blow, at most 6.5; nausea 4.5s | Furfur Requiem: Chord of Ruin: nova, reach 17, damage 23, nausea 9s |
| 8 | Marchosias, Hunter of Embers | Heat | 0xE23A22 / 0xFFA878 | spike membrane flame none ears | Brand: +30% of the blow, at most 7.5; glowing 7.5s | Marchosias Hunt: Emberfang Run: beam, reach 22, damage 36, burn 8s |
| 9 | Sitri, Dawn's Radiance | Light | 0xFFE070 / 0xFFFFFF | crown feather none ring no ears | Smite: +30% of the blow, at most 7.5; glowing 5s | Sitri Dawn: Radiance of First Light: nova, reach 14, damage 27, blindness 5s |
| 10 | Bifron, Stormfang | Lightning | 0x7B4DFF / 0xD2C2FF | stag blade blade spokes no ears | Paralyze: +30% of the blow, at most 7.5; slowness 1.5s | Bifron Skyfall: Pillars of Storm: columns, reach 14, damage 27, slowness 3s |
| 11 | Paimon, Sovereign of the Sky | Wind | 0x4FC3D6 / 0xDFFAFF | crescent feather whip runes ears | Slice: +30% of the blow, at most 7.5; wither 3s | Paimon Tempest: Heavenly Gale: vortex, reach 15, damage 25, no effect |
| 12 | Vassago, Lord of the Abyss | Water | 0x2F55D8 / 0xB8CCFF | ram membrane tendril none no ears | Drown: +30% of the blow, at most 7.5; slowness 4.5s | Vassago Maelstrom: Abyssal Tide: vortex, reach 16, damage 27, slowness 6s |
| 13 | Bael, King of the First Throne (new) | Strength | 0xC9A227 / 0xFFF0A0 | crown blade none spokes no ears | Kneel: +30% of the blow, at most 7.5; slowness 4.5s | Bael Thronus: Edict of the First King: quake, reach 15, damage 30, slowness 5s |
| 14 | Agares, Rider of Crocodile Tides (new) | Water | 0x1FA37A / 0xB8FFE0 | ram veil blade none no ears | Drag: +25% of the blow, at most 6.5; slowness 4s | Agares Maw: Jaws of the Rising Tide: cone, reach 17, damage 30, slowness 5s |
| 15 | Valefor, Thief of Storm-Hearts (new) | Lightning | 0x5A3DB5 / 0xE0D2FF | spike blade whip ring ears | Plunder: +25% of the blow, at most 6.5; weakness 3s | Valefor Larceny: Storm-Heart Heist: rain, reach 14, damage 24, weakness 6s |
| 16 | Barbatos, Hunter of the Wild Green (new) | Life | 0x2E7D32 / 0xCFFFB0 | stag veil whip none ears | Hunt: +28% of the blow, at most 7; glowing 7.5s | Barbatos Venatio: The Wild Hunt: columns, reach 15, damage 26, poison 6s |
| 17 | Gusion, Seer of the Tide Mirror (new) | Water | 0x3D7EAA / 0xD6F2FF | crescent veil none runes no ears | Foresight: +25% of the blow, at most 6.5; glowing 6s | Gusion Speculum: Verdict of the Tide Mirror: cross, reach 18, damage 28, glowing 8s |
| 18 | Vepar, Mermaid Sovereign of Wrecks (new) | Water | 0x00A6B8 / 0xBFFCFF | none veil tendril ring ears | Wreck: +28% of the blow, at most 7; fragility 7.5s | Vepar Naufragium: Wrecker's Surge: wave, reach 17, damage 26, fragility 8s |
| 19 | Shax, Silencer of the Dove (new) | Sound | 0x8E8EA8 / 0xF0F0FF | none feather whip ring ears | Hush: +22% of the blow, at most 6; slowness 3s | Shax Silentium: Hush of the White Dove: nova, reach 16, damage 22, slowness 6s |
| 20 | Buer, Healer of the Wheel (new) | Life | 0x9CCC3B / 0xF4FFC0 | stag feather none ring ears | Mend: +22% of the blow, at most 6; weakness 3s | Buer Rota: The Wheel of Mending: heal_field, reach 14, damage 17, weakness 6s |
| 21 | Andrealphus, Peacock of the Stars (new) | Light | 0x2F6FDB / 0xBFE3FF | crown feather whip spokes ears | Dazzle: +28% of the blow, at most 7; blindness 2.5s | Andrealphus Pavo: Fall of a Hundred Stars: rain, reach 16, damage 27, blindness 4s |
| 22 | Kimaris, Rider of the Dark Steed (new) | Strength | 0x3A2A3F / 0xFF6A8A | spike membrane whip none no ears | Trample: +30% of the blow, at most 8; slowness 2s | Kimaris Equus: Charge of the Dark Steed: beam, reach 22, damage 35, slowness 3s |
| 23 | Amdusias, Maestro of the Unicorn Horn (new) | Sound | 0xD58AF0 / 0xFFE3FF | spike feather tendril runes no ears | Shockwave: +25% of the blow, at most 6.5; nausea 4s | Amdusias Concerto: The Unicorn's Last Chord: cross, reach 17, damage 26, nausea 6s |
| 24 | Belial, Lord of Hollow Thrones (new) | Heat | 0x7A0F2E / 0xFF7096 | crown fire flame none no ears | Cinder: +30% of the blow, at most 7.5; burn 7s | Belial Inanis: Fall of the Hollow Throne: implode, reach 14, damage 32, burn 8s |
| 25 | Decarabia, Star Crab of the Deep (new) | Light | 0xE0B45A / 0xFFF4C8 | none membrane blade spokes ears | Shell: +25% of the blow, at most 6.5; slowness 2.5s | Decarabia Astra: Star-Shell Nova: nova, reach 14, damage 26, slowness 5s |
| 26 | Seere, Swift Wind Prince (new) | Wind | 0xE8E8C0 / 0xFFFFFF | none feather whip spokes ears | Quicken: +28% of the blow, at most 7; no effect | Seere Celer: Passage of the Swift Wind: beam, reach 24, damage 28, slowness 2s |
| 27 | Dantalion, Many-Faced Scholar (new) | Light | 0xC0A0FF / 0xF2E8FF | crown veil none runes ears | Beguile: +25% of the blow, at most 6.5; nausea 5s | Dantalion Facies: Lecture of a Thousand Faces: columns, reach 14, damage 25, nausea 7s |
| 28 | Andromalius, Seeker of Hidden Things (new) | Lightning | 0x2B2B6B / 0x9CA8FF | stag blade tendril runes no ears | Reveal: +27% of the blow, at most 7; glowing 7.5s | Andromalius Inquisitio: Thunder Finds the Hidden: bolts, reach 16, damage 26, glowing 8s |
| 29 | Glasya-Labolas, Hound of Slaughter (new) | Strength | 0xA00000 / 0xFF9090 | ram membrane whip none no ears | Bloodlust: +25% of the blow, at most 7; wither 3s | Glasya-Labolas Cruor: The Slaughter Howl: cone, reach 15, damage 34, weakness 5s |
| 30 | Haagenti, Alchemist of the Winged Bull (new) | Strength | 0xB87333 / 0xFFD9A0 | crescent feather whip none ears | Transmute: +27% of the blow, at most 7; no effect | Haagenti Opus: The Great Work: vortex, reach 15, damage 25, poison 5s |
| 31 | Crocell, Frost Sage (new) | Water | 0x9ADCF0 / 0xFFFFFF | crown veil none ring ears | Rime: +22% of the blow, at most 6; slowness 5s | Crocell Glacies: The Sage's Frozen Tide: wave, reach 16, damage 24, slowness 7s |
| 32 | Furcas, Old Knight of the Ice Wind (new) | Wind | 0xAAB7C8 / 0xFFFFFF | stag blade none ring no ears | Icewind: +27% of the blow, at most 7; slowness 3.5s | Furcas Boreas: Charge of the Ice Wind: cone, reach 18, damage 29, slowness 6s |
| 33 | Balam, Three-Headed Prophet (new) | Heat | 0xD1622A / 0xFFE0A0 | ram fire whip spokes ears | Triple: +25% of the blow, at most 6.5; burn 5s | Balam Trias: Prophecy of Three Flames: columns, reach 15, damage 28, burn 7s |
| 34 | Alloces, Lion-Eyed Knight (new) | Heat | 0xE8892B / 0xFFF0B0 | crown fire whip ring ears | Roar: +27% of the blow, at most 7; weakness 4s | Alloces Leonis: The Lion's Judgement: nova, reach 15, damage 27, weakness 6s |
| 35 | Camio, Songbird of the Crows (new) | Sound | 0x2A3340 / 0xA0C8FF | none feather blade runes ears | Carrion: +25% of the blow, at most 6.5; poison 3s | Camio Corvus: A Murder of Crows: rain, reach 15, damage 24, fear 5s |
| 36 | Murmur, Death Drummer (new) | Sound | 0x6B3FA0 / 0xE8C8FF | crescent membrane tendril spokes no ears | Drumbeat: +22% of the blow, at most 7; slowness 2s | Murmur Tympanum: The Death Drum: quake, reach 17, damage 28, nausea 7s |

The cap of a row is for mobs: a rider on a player is never more than 6 points (`TraitRules.PLAYER_RIDER_CAP`), whatever the row or a dark Djinn says.

## 2. What the new traits do

The share of the blow and the cap are the row's; the rest is the special (`TraitRules` for what changes the share, `DjinnTraits` for what else the blow does). Nothing places or breaks a block, and no item is destroyed (a wrecked armour piece keeps one point of durability).

| Special | Djinn | Besides the rider |
|---|---|---|
| Kneel | Bael | the foe is slammed down (its motion is cut and it is pushed into the ground) and slowed |
| Drag | Agares | the foe is pulled towards you (never past you) |
| Plunder | Valefor | up to two beneficial effects are taken off the foe and you get Speed for as many seconds |
| Hunt | Barbatos | a foe below half health takes half again as much rider; the foe glows |
| Foresight | Gusion | the foe is revealed (glowing) and you get Resistance for three seconds |
| Wreck | Vepar | each armour piece the foe wears loses a fiftieth of its durability (never broken) and the foe is cracked (Fragility) |
| Hush | Shax | the foe is silenced and slowed, and a mob forgets whom it was fighting |
| Mend | Buer | half the rider heals you and four tenths of it each ally within six blocks |
| Dazzle | Andrealphus | the foe is blinded and up to eight foes within 3.5 blocks of it glow |
| Trample | Kimaris | the rider grows with your speed (up to +80%, a sprint is about +70%) and the foe is shoved |
| Shockwave | Amdusias | up to eight foes within 3.5 blocks of the target are shoved away from it |
| Cinder | Belial | the target burns longer and up to three foes within 3.5 blocks catch fire |
| Shell | Decarabia | each blow adds one point of Absorption to you, up to six |
| Quicken | Seere | each blow gives you Speed II for two seconds |
| Beguile | Dantalion | a mob turns on its nearest neighbour within eight blocks |
| Reveal | Andromalius | invisibility is stripped and an invisible foe takes half again as much rider |
| Bloodlust | Glasya-Labolas | each blow within four seconds of the last adds 10% to the rider, up to +40% |
| Transmute | Haagenti | a random boon (Speed, Strength, Regeneration or Resistance) for you and a random bane (Slowness, Weakness or Poison) for the foe |
| Rime | Crocell | frost spreads: up to eight foes within 3.5 blocks are frozen a little and slowed |
| Icewind | Furcas | the foe is thrown back and chilled |
| Triple | Balam | up to two foes within four blocks take 60% of the rider and burn |
| Roar | Alloces | up to eight foes within five blocks of you are weakened and shoved |
| Carrion | Camio | a foe with any harmful effect takes half again as much rider |
| Drumbeat | Murmur | every third blow within four seconds of the last has double rider and a shove |

## 3. The new Extreme Magic shapes

Appended after the original seven (Nova, Bolts, Columns, Wave, Vortex, Beam, Heal Field). `Shape.steps()` is the number of strikes (four ticks apart), `Shape.maxScale()` the most one target can take in one casting as a multiple of the row's damage; the table test keeps `damage x maxScale` at or under 48.

| Shape | Steps | Max scale | What it does | Used by |
|---|---|---|---|---|
| Rain | 14 | 1.0 | fourteen marked drops on a golden-angle spiral out from you, each 0.5 of the damage on foes within 2.6 blocks (a target is hit twice at most) | Valefor, Andrealphus, Camio |
| Cone | 1 | 1.0 | a 110-degree fan in front of you out to the reach, weaker at the edge, shoving foes forward | Agares, Glasya-Labolas, Furcas |
| Quake | 3 | 1.0 | three rings in turn (inner, middle, outer third of the reach), each hurting and throwing up the foes in its band | Bael, Murmur |
| Cross | 1 | 1.0 | four arms (forward, back, left, right), 4.4 blocks wide, out to the reach | Gusion, Amdusias |
| Implode | 2 | 1.3 | the first strike drags every foe in the reach towards you (no damage), the second collapses the hollow: 1.3 at the middle to 0.7 at the edge | Belial |

## 4. The weapons

`djinn_weapon_<id>` and `djinn_weapon_<id>_dark`, 72 items. Damage and speed are melee numbers; dark is x1.1 damage. Costs are shares of the maximum Magoi (dark x1.25), power is raw damage through the balance law (Full Equip x1.35, dark x1.2). Act one is a right-click, act two a sneak + right-click in the Full Equip. Engravings are Tensura's (CLAUDE.md rule 6).

| Djinn | Weapon (light / dark) | Kind | Damage | Speed | Reach | Act 1 (cost, cooldown, power, reach, shape) | Act 2 | Engravings |
|---|---|---|---|---|---|---|---|---|
| Baal | Baal's Thunder Lance / Baal's Black Thunder Lance | lance | 10 | 1.4 | +1 | thunder_throw (3%, 5.4s, 10.5, 22 b, hand-written) | judgement_step (5%, 11.7s, 12.5, 9 b, hand-written) | elemental_boost 2, barrier_piercing 1, swift 1 |
| Amon | Amon's Flame Greatsword / Amon's Black Sun Greatsword | greatsword | 13 | 1 | +0.5 | sun_cleave (3%, 5s, 12.5, 6.5 b, hand-written) | ember_wall (5%, 12.5s, 9, 7.5 b, hand-written) | elemental_boost 2, severance 2, sturdy 1 |
| Vinea | Vinea's Tide Trident / Vinea's Drowned Trident | trident | 10 | 1.5 | +0.8 | tide_spear (2.5%, 4.5s, 9, 12 b, hand-written) | frost_surge (5%, 11.7s, 10.5, 7.5 b, hand-written) | elemental_boost 2, magic_weapon 1, magic_protection 1 |
| Focalor | Focalor's Gale Glaive / Focalor's Black Gale Glaive | glaive | 9 | 1.6 | +1 | cyclone_sweep (3%, 5s, 9, 5 b, hand-written) | updraft_leap (4%, 10s, 7, 5.5 b, hand-written) | swift 2, elemental_boost 1, magic_weapon 1 |
| Zagan | Zagan's Bloom Staff / Zagan's Withered Staff | staff | 7 | 1.6 | +0.5 | bloom_heal (4%, 8.1s, 7, 10 b, hand-written) | thorn_snare (4%, 10.8s, 8, 13 b, hand-written) | vitality 2, restoration 1, elemental_boost 1 |
| Zepar | Zepar's Titan Axe / Zepar's Black Titan Axe | axe | 14 | 0.9 | +0.3 | titan_slam (4%, 7.2s, 13.5, 6 b, hand-written) | rock_breaker (4%, 10.8s, 13.5, 5 b, hand-written) | crushing 3, sturdy 2, severance 1 |
| Furfur | Furfur's War Horn / Furfur's Black War Horn | horn | 8 | 1.2 | +0.3 | resonance_wave (3%, 5.4s, 8, 10 b, hand-written) | dissonance (4.5%, 11.7s, 7, 7 b, hand-written) | magic_interference 2, elemental_boost 1, enervation 1 |
| Marchosias | Marchosias' Ember Claws / Marchosias' Black Ember Claws | claws | 8.5 | 2.4 | +0 | ember_rush (3%, 5.4s, 9, 9 b, hand-written) | brand (3.5%, 9s, 6, 15 b, hand-written) | swift 2, energy_steal 1, severance 1 |
| Sitri | Sitri's Dawn Bow / Sitri's Dusk Bow | bow | 7 | 1.8 | +0.5 | dawn_arrow (2.5%, 2.7s, 11.5, 35 b, hand-written) | radiant_burst (5%, 11.7s, 9, 7 b, hand-written) | holy_weapon 2, elemental_boost 2, barrier_piercing 1 |
| Bifron | Bifron's Storm Glaive / Bifron's Black Storm Glaive | glaive | 10 | 1.5 | +1 | chain_lightning (3%, 5.4s, 9, 13 b, hand-written) | storm_pillars (5%, 11.7s, 11.5, 15 b, hand-written) | elemental_boost 2, barrier_piercing 1, swift 1 |
| Paimon | Paimon's Twin-Moon Glaive / Paimon's Black Twin-Moon Glaive | glaive | 10 | 1.5 | +1 | crescent_wind (3%, 5s, 9, 11 b, hand-written) | heavenly_dash (4%, 10s, 10.5, 10 b, hand-written) | swift 2, severance 2, elemental_boost 1 |
| Vassago | Vassago's Abyssal Scythe / Vassago's Black Abyssal Scythe | scythe | 11 | 1.1 | +0.8 | abyssal_reap (3.5%, 6.3s, 10.5, 6 b, hand-written) | drowning_pull (4.5%, 11.7s, 7, 10 b, hand-written) | soul_eater 1, energy_steal 1, enervation 2, severance 1 |
| Bael | Bael's Crown Claymore / Bael's Black Crown Claymore | claymore | 12 | 1 | +0.5 | royal_decree (3%, 5.5s, 11, 6.5 b, cone) | throne_quake (5%, 13.5s, 10, 6.5 b, nova) | crushing 2, severance 2, sturdy 2, vitality 1 |
| Agares | Agares' Riptide Harpoon / Agares' Drowned Harpoon | harpoon | 9.5 | 1.3 | +1 | tidal_lunge (3%, 6s, 9.5, 8 b, dash) | undertow (4.5%, 13s, 7.5, 8 b, pull) | elemental_boost 2, swift 1, magic_weapon 1 |
| Valefor | Valefor's Stormheart Daggers / Valefor's Black Stormheart Daggers | daggers | 7 | 2.2 | +0 | spark_snatch (2.8%, 4.5s, 8, 12 b, chain) | storm_heist (4%, 11s, 9, 10 b, dash) | swift 2, energy_steal 1, elemental_boost 1, barrier_piercing 1 |
| Barbatos | Barbatos' Greenwood Bow / Barbatos' Withered Greenwood Bow | bow | 6.5 | 1.7 | +0.5 | wild_arrow (2.5%, 3s, 10, 30 b, beam) | green_ambush (4.5%, 12.5s, 8, 14 b, pillars) | swift 1, elemental_boost 2, vitality 1, magic_weapon 1 |
| Gusion | Gusion's Mirrorwater Rapier / Gusion's Black Mirrorwater Rapier | rapier | 8 | 1.7 | +0.8 | mirror_thrust (2.8%, 4.5s, 9, 10 b, beam) | tide_mirror (4%, 12s, 7, 6 b, shield) | elemental_boost 2, magic_protection 2, swift 1 |
| Vepar | Vepar's Wreckwater Anchor / Vepar's Black Wreckwater Anchor | anchor | 12 | 0.9 | +0.3 | anchor_drop (3.5%, 6.5s, 11, 5 b, nova) | shipwreck (4.5%, 13s, 8, 8 b, wall) | crushing 2, sturdy 2, elemental_boost 1, severance 1 |
| Shax | Shax's Dove-Feather Fans / Shax's Black Dove-Feather Fans | fans | 6.5 | 2 | +0 | wing_hush (2.8%, 4.5s, 7.5, 7 b, cone) | muted_flurry (4.5%, 12.5s, 7, 6 b, nova) | magic_interference 2, swift 2, enervation 1 |
| Buer | Buer's Wheel Staff / Buer's Black Wheel Staff | staff | 6 | 1.5 | +0.5 | wheel_mend (4%, 8.5s, 7, 9 b, heal) | cleansing_spokes (4.5%, 13s, 8, 11 b, heal) | restoration 2, vitality 2, elemental_boost 1 |
| Andrealphus | Andrealphus' Starfall Lance / Andrealphus' Black Starfall Lance | lance | 8.5 | 1.4 | +1 | star_spear (2.8%, 3.5s, 9.5, 26 b, beam) | peacock_fan (4.5%, 12.5s, 8.5, 8 b, cone) | holy_weapon 1, elemental_boost 2, barrier_piercing 1, magic_weapon 1 |
| Kimaris | Kimaris' Dark Steed Lance / Kimaris' Black Steed Lance | lance | 10.5 | 1.2 | +1.2 | charge_lance (3.5%, 6.5s, 11, 10 b, dash) | dark_gallop (4.8%, 12.5s, 12, 13 b, dash) | crushing 2, swift 2, severance 1, sturdy 1 |
| Amdusias | Amdusias' Alicorn Rapier / Amdusias' Black Alicorn Rapier | rapier | 7.5 | 1.5 | +0.6 | horn_note (2.8%, 4s, 8, 16 b, beam) | grand_chord (4.5%, 13s, 8, 7.5 b, nova) | magic_interference 1, elemental_boost 2, swift 1, enervation 1 |
| Belial | Belial's Hollow Throne Blade / Belial's Black Hollow Throne Blade | greatsword | 12.5 | 0.95 | +0.4 | hollow_cleave (3.2%, 6s, 11.5, 6.5 b, cone) | throne_ash (4.8%, 13.5s, 9, 6.5 b, nova) | severance 2, elemental_boost 2, soul_eater 1, enervation 1 |
| Decarabia | Decarabia's Star-Shell Pincers / Decarabia's Black Star-Shell Pincers | pincers | 7.5 | 1.6 | +0.2 | pincer_snap (3%, 5s, 10, 5 b, cone) | star_shell (4%, 12s, 6, 6 b, shield) | sturdy 2, holy_coat 1, elemental_boost 2, magic_protection 1 |
| Seere | Seere's Swift Wind Sabre / Seere's Black Wind Sabre | sabre | 7 | 2 | +0.4 | swift_slash (2.6%, 3.5s, 8, 7 b, dash) | gale_step (4%, 10s, 8.5, 12 b, dash) | swift 3, elemental_boost 1, severance 1 |
| Dantalion | Dantalion's Scholar Staff / Dantalion's Black Scholar Staff | staff | 6 | 1.6 | +0.5 | forbidden_page (2.8%, 4.5s, 8, 14 b, beam) | many_faces (4.5%, 13s, 8, 14 b, pillars) | magic_interference 2, magic_weapon 1, elemental_boost 1, magic_capacity 1 |
| Andromalius | Andromalius' Seeker Spear / Andromalius' Black Seeker Spear | spear | 9 | 1.5 | +1 | seeker_bolt (2.8%, 4s, 9, 24 b, beam) | unveil (4.2%, 12s, 7.5, 9 b, nova) | barrier_piercing 2, elemental_boost 2, swift 1, magic_interference 1 |
| Glasya-Labolas | Glasya-Labolas' Slaughter Fangs / Glasya-Labolas' Black Slaughter Fangs | claws | 8 | 2.2 | +0 | savage_rend (2.8%, 4s, 9, 4.5 b, cone) | bloodhound_rush (4.2%, 11s, 10, 9 b, dash) | severance 2, energy_steal 1, swift 2, crushing 1 |
| Haagenti | Haagenti's Bull Cleaver / Haagenti's Black Bull Cleaver | cleaver | 11.5 | 1.1 | +0.5 | bull_gore (3.4%, 6s, 11, 7 b, dash) | great_work (4.5%, 13s, 8, 6.5 b, nova) | crushing 2, sturdy 1, vitality 1, elemental_boost 1 |
| Crocell | Crocell's Frost Sage Staff / Crocell's Black Frost Staff | staff | 6.5 | 1.5 | +0.5 | frost_bolt (2.6%, 3.5s, 8, 20 b, beam) | rime_field (4.5%, 13s, 7.5, 7.5 b, nova) | elemental_boost 2, lethargy 1, magic_weapon 1, magic_protection 1 |
| Furcas | Furcas' Ice-Wind Halberd / Furcas' Black Ice-Wind Halberd | halberd | 10 | 1.2 | +1 | ice_wind_sweep (3.2%, 6s, 9.5, 6.5 b, cone) | knights_gale (4.5%, 12s, 10, 9 b, dash) | elemental_boost 2, stagnation 1, sturdy 1, swift 1 |
| Balam | Balam's Triple-Flame Trident / Balam's Black Triple-Flame Trident | trident | 9 | 1.4 | +0.8 | triple_flame (3%, 5s, 9, 11 b, chain) | prophecy (4.8%, 13s, 10, 14 b, pillars) | elemental_boost 2, severance 1, swift 1, magic_weapon 1 |
| Alloces | Alloces' Lion-Eye Longsword / Alloces' Black Lion-Eye Longsword | longsword | 10 | 1.3 | +0.6 | lion_slash (3%, 5s, 10.5, 6 b, cone) | lions_roar (4.5%, 12.5s, 7, 8.5 b, nova) | severance 2, elemental_boost 2, sturdy 1, swift 1 |
| Camio | Camio's Raven Quill Staff / Camio's Black Raven Quill Staff | staff | 6.5 | 1.7 | +0.5 | crow_flock (2.8%, 4s, 7.5, 12 b, chain) | murder_of_crows (4.5%, 12.5s, 8.5, 14 b, pillars) | magic_interference 1, swift 2, elemental_boost 1, soul_eater 1 |
| Murmur | Murmur's Death Drum Mace / Murmur's Black Death Drum Mace | mace | 11 | 1.1 | +0.3 | death_drum (3.4%, 6s, 9.5, 5.5 b, nova) | funeral_march (4.7%, 13.5s, 8.5, 9 b, pull) | soul_eater 1, enervation 2, magic_interference 1, crushing 1 |

The generic acts are `ActLibrary` (ten shapes: beam, cone, nova, dash, wall, pillars, chain, heal, pull, shield, each with its own effects, shove and fire) run by `DjinnWeaponActs.generic`; the twelve original weapons keep their hand-written acts. Item models, icons and meshes are produced by the asset tools (`tools/relics/`); an item without a model renders as the missing-model cube and nothing else breaks.

## 5. The buff of the twelve (old -> new)

Owner request: better Half Equip, Extreme Magic and Djinn weapon acts. Rules of the buff: rider share +0.05, rider cap +1.5, effect lengths x1.5, Extreme reach +1 to +2 blocks and damage about +13% (never more than a fifth), weapon melee +1, act power x1.15, cooldown x0.9, reach x1.1. Costs, speeds and the Full / dark multipliers are unchanged. The balance law and its caps are untouched: a rider on a player is still at most 6, an Extreme Magic on a player at most 45% of their health, an act at most 45%.

| Djinn | Rider share | Rider cap | Trait effect | Extreme reach | Extreme damage | Extreme effect | Melee | Act 1 power / cooldown / reach | Act 2 power / cooldown / reach |
|---|---|---|---|---|---|---|---|---|---|
| Baal | 0.25 -> 0.3 | 6 -> 7.5 | 2s -> 3s | 14 -> 15 | 26 -> 30 | 3s -> 4.5s | 9 -> 10 | 9/6s/20 -> 10.5/5.4s/22 | 11/13s/8 -> 12.5/11.7s/9 |
| Amon | 0.3 -> 0.35 | 6 -> 8 | 3s -> 4.5s | 12 -> 13 | 28 -> 32 | 6s -> 8s | 12 -> 13 | 11/5.5s/6 -> 12.5/5s/6.5 | 8/14s/7 -> 9/12.5s/7.5 |
| Vinea | 0.2 -> 0.25 | 5 -> 6.5 | 3s -> 4.5s | 15 -> 16 | 22 -> 25 | 4s -> 6s | 9 -> 10 | 8/5s/11 -> 9/4.5s/12 | 9/13s/7 -> 10.5/11.7s/7.5 |
| Focalor | 0.25 -> 0.3 | 6 -> 7.5 | 0s -> 0s | 14 -> 15 | 20 -> 23 | 5s -> 7.5s | 8 -> 9 | 8/5.5s/4.5 -> 9/5s/5 | 6/11s/5 -> 7/10s/5.5 |
| Zagan | 0.2 -> 0.25 | 5 -> 6.5 | 0s -> 0s | 12 -> 14 | 16 -> 18 | 5s -> 7.5s | 6 -> 7 | 6/9s/9 -> 7/8.1s/10 | 7/12s/12 -> 8/10.8s/13 |
| Zepar | 0.3 -> 0.35 | 6 -> 8 | 3s -> 4.5s | 10 -> 11 | 30 -> 34 | 2s -> 3s | 13 -> 14 | 12/8s/5.5 -> 13.5/7.2s/6 | 12/12s/4.5 -> 13.5/10.8s/5 |
| Furfur | 0.2 -> 0.25 | 5 -> 6.5 | 3s -> 4.5s | 16 -> 17 | 20 -> 23 | 6s -> 9s | 7 -> 8 | 7/6s/9 -> 8/5.4s/10 | 6/13s/6.5 -> 7/11.7s/7 |
| Marchosias | 0.25 -> 0.3 | 6 -> 7.5 | 5s -> 7.5s | 20 -> 22 | 32 -> 36 | 6s -> 8s | 7.5 -> 8.5 | 8/6s/8 -> 9/5.4s/9 | 5/10s/14 -> 6/9s/15 |
| Sitri | 0.25 -> 0.3 | 6 -> 7.5 | 3s -> 5s | 13 -> 14 | 24 -> 27 | 3s -> 5s | 6 -> 7 | 10/3s/32 -> 11.5/2.7s/35 | 8/13s/6.5 -> 9/11.7s/7 |
| Bifron | 0.25 -> 0.3 | 6 -> 7.5 | 1s -> 1.5s | 13 -> 14 | 24 -> 27 | 2s -> 3s | 9 -> 10 | 8/6s/12 -> 9/5.4s/13 | 10/13s/14 -> 11.5/11.7s/15 |
| Paimon | 0.25 -> 0.3 | 6 -> 7.5 | 2s -> 3s | 14 -> 15 | 22 -> 25 | 0s -> 0s | 9 -> 10 | 8/5.5s/10 -> 9/5s/11 | 9/11s/9 -> 10.5/10s/10 |
| Vassago | 0.25 -> 0.3 | 6 -> 7.5 | 3s -> 4.5s | 15 -> 16 | 24 -> 27 | 4s -> 6s | 10 -> 11 | 9/7s/5.5 -> 10.5/6.3s/6 | 6/13s/9 -> 7/11.7s/10 |

Besides the numbers: Chain Lightning's blows chain further (Baal's rider reaches five blocks), Melt burns 7 s, Freeze freezes harder, Zagan's lifesteal returns 30% of the blow, Deafen and Brand last longer, Drown takes more breath, the Zagan Heal Field heals 8 (was 6) with a longer Regeneration. In the weapons: Thunder Throw jumps one more link and slows longer, Judgement Step leaves you quick, the fire acts burn 7 to 8 s, Frost Surge freezes harder, Updraft Leap floats longer and hits harder, Bloom Heal regenerates longer, the control acts last longer, Brand marks for +30% (+15% on players), Dawn Arrow glows longer and its second hit is 70%, Radiant Burst heals 45%, Chain Lightning has five links (six in the Full Equip), Abyssal Reap heals 30% and Drowning Pull takes more breath.

## 6. What is checked

`DjinnTableTest` (36 rows in the owner's order and exactly as specified, distinct names, colours, forms and looks, a special of its own for each new Djinn, every shape used, the buff never weaker), `DjinnTraitRulesTest` (what each special reads, the 6-point clamp on players, the blow-streak table), `DjinnWeaponRulesTest` (72 ids, 72 acts, every generic act sane, the twelve never weaker, engravings are Tensura's), `WeaponLoanVesselTest` / `WeaponLoanOverlayTest` (the vessel overlay) and the self-test `djinn.core_rebuild` in the server smoke test. Where a bound was widened by the buff the test says so in its comment.
