# Damage scale-down: 10,000 a hit, 15,000 for one weapon, the Djinn band 4,000 to 8,000

Owner request: "scale down the damage ... 20K is a little insane"; the Djinn weapons ("Jin", the Djinn of Magi) hit 4,000 (the weakest) to 8,000 (the strongest) by rank; "damage in general
shouldn't go higher than 10K, besides maybe the strongest weapon"; "the highest should be 15K"; each Djinn's Extreme Magic rebuilt on its own element with the exact damage type; rankings for
everything (see `docs/rankings.md`).

**Everything here is a SERVER setting of its own** (`nusmp-damage-server.toml`, section `[damage]`). `scaleDownEnabled = false` gives the old numbers exactly. Nothing could be played here; CI
compiles and runs the pure tests and the server self-tests `dmgscale.*`.

## 1. Decisions and readings (for the owner)

* **"Gens" / "Jin" = the Djinn of Magi**, not Genesis (corrected by the owner). The 4,000-8,000 band is for the 36 Djinn weapons (and their Extreme Magic). Genesis weapons are NOT in the band: they
  only live under the 10,000 ceiling (table in section 3).
* **"Extreme magic" = the Djinn's Extreme Magic** (`Djinn.Extreme`, phase 4 of the Djinn system: Baal Bolg, Amon Salos ...).
* **"Escadosa" is read as Elsdocia**, the first Wizard King's sword (`MagicWeaponItem.Kind.ELSDOCIA`, item `anime_multiverse:elsdocia`): the one Black Clover sword the mod already treats as the
  strongest weapon (Genesis grade, 400 base damage, the only item `GradeDamage` exempts, it grows +25 per Grand Magic owner). No item or text in the repo is spelled "Escadosa", "Escanor's
  Rhitta" is a boss move (`arc/sds/`), not an item. **Candidates considered:** Elsdocia (picked: the strongest weapon in every table), the Genesis Demon-Slayer Sword (`demon_slayer_sword`, the
  Asta sword, strongest of the Genesis relics but under Elsdocia's 400), the Demon Dweller / Destroyer / Licht family (Legend), Astralis (Genesis relic). **Decision for the owner:** if "Escadosa"
  is a different sword, change `damage.apexWeaponId` (an item id, no code change).
* **15,000 is the owner's number** (confirmed): the hard maximum of one hit anywhere in the mod. It is one constant, `DamageScale.ABSOLUTE_MAX`; every setting is clamped to it in code, so raising
  `apexHitCap` or `globalHitCap` in the file cannot pass it.
* **Hits that count as "from the mod"**: any hit a player deals (so Tensura's own skills used by a player are capped too), any hit by or through an entity of the mod (bosses on players), any
  damage type of the mod. A vanilla zombie hitting you, fall damage, `/kill` and Tensura's own mobs are never touched.
* **Ceiling, not a rewrite of the formulas.** Every old formula (EP scale x12, late-game x6, the power raise x25, the hero buff x4 x1.5 ...) is kept; the last word on one hit is a ceiling. A side effect
  to know: past the ceiling a stronger player hits the same number. If the owner wants the curve itself lowered (so strength still shows below the cap), that is a follow-up (lower
  `lateGameScalingCap`, `djinnFactor`, `vassalFactor`: all existing settings).
* **The Djinn floor.** A full swing with a Djinn weapon never lands under 4,000 against mobs (`damage.djinnFloor`, default on; a partly charged swing is scaled like vanilla; against players the
  PvP hit cap decides, no floor). Off = the band is only a ceiling.
* **Wiki research (rule 10):** `magi.fandom.com` (English) was blocked in this session (host not in the allowed network list), and no Japanese wiki was tried after that. The Djinn order is the
  owner's own list (Baal, Paimon, Valefor, Zagan, Vinea, Amon, Focalor, Furcas, then Foras which is not in the mod's 36) followed by my reading of Magi canon for the other 28. **Not wiki-sourced; the
  owner should read the table in section 4 and reorder** (it is one list in `assets/anime_multiverse/rankings.json`).
* **Elements are the owner's examples where given** (Baal thunder, Amon fire, Vinea ice, Focalor water, Furcas wind, Zagan life/healing) and the mod's own description of the spell otherwise. They
  differ from the mod's old *affinity* for Vinea (water), Focalor (wind) and a few others; the affinity (looks, the bearer's calm) is unchanged, only the Extreme Magic damage type follows the
  concept.

## 2. Settings (`nusmp-damage-server.toml`, `[damage]`)

| Key | Default | Meaning |
|---|---|---|
| `scaleDownEnabled` | true | Master switch. false = the old damage exactly. |
| `globalHitCap` | 10000 | Most one hit from the mod may deal (max 15000). |
| `weaponHitCap` | 10000 | Most one melee blow with a held weapon of the mod may deal (max 15000). |
| `djinnMin` / `djinnMax` | 4000 / 8000 | The Djinn band: rank 36 / rank 1. |
| `djinnFloor` | true | A full Djinn weapon swing lands at least `djinnMin` (mobs). |
| `apexWeaponId` | `anime_multiverse:elsdocia` | The one weapon above `globalHitCap`. Empty = none. |
| `apexHitCap` | 15000 | Its ceiling (clamped to 15000 in code). |
| `extremeElementTypes` | true | Extreme Magic hits with its element's damage type; false = the old types. |

**To restore the old numbers:** set `scaleDownEnabled = false` (everything: ceilings, band, apex, element types). To keep the ceilings and only restore the old Extreme Magic types set
`extremeElementTypes = false`. Existing settings that shape the numbers (`lateGameScaling*`, `power.*`, `heroweapon.buff.*`, `pvpHitCapPercent`) are untouched.

## 3. Audit: every source that can reach about 20,000 a hit

Old max = computed from the formulas at the top of each range (nothing was observed in a game). New max = under the defaults.

| Source | Old max per hit | New max | How |
|---|---|---|---|
| Balance law (`BalanceLaw.damage`): raw x EP scale (x12) x late-game (x6) x mastery (1.15) x mana law | raw 18..48 -> about 1,500 to 4,000; relic acts raw up to 400 -> 33,000 (the relic `Fx.hit` clamps raw to 400) | 10,000 (15,000 only for the apex weapon's wielder) | `BalanceLaw.cap` applies the widest ceiling; the event applies the exact one |
| Ultimate classes (`classScale`, x24 to x36) | raw x36 -> about 2,000 to 17,000 | 10,000 | same |
| Power raise 3.74.0 (`power/PowerEvents`: Djinn x25, Beast Will x25, Vassal x25, Legendary x37.5) | a 400 relic hit x25 = 10,000; Legendary x37.5 = 15,000; settings allow x1000 | 10,000 | event ceiling (it runs after the power raise) |
| Hero weapon buff (x4, Holy x1.5 more), stacked with the power raise | Legendary Sword 152 x 4 x 1.5 x 37.5 = about 34,000 | 10,000 | event ceiling |
| Conceptual Severance (`DemonSlayerSwordItem.genesis`: `EP^0.6 / 2` x affinity 1.75 x 1.25) | 17,000 at 10M EP, 69,000 at 100M EP, unbounded against a mob | 10,000 (`BalanceLaw.cap`, then the event); players still the 40% PvP cap, bosses 15% of their health | ceiling |
| Genesis relic weapons (attack stat 260-350, aura x1.6, tier x1.65, then the power factor) | 560 stat x 25 = 14,000 and up | 10,000 | event ceiling |
| Elsdocia (400+, +25 per Grand owner, then arts through the balance law) | 400 x 37.5 = 15,000 and up; arts 33,000 | 15,000 (apex) | `apexWeaponId` / `apexHitCap`; clamped to 15,000 |
| Djinn weapon melee (stat 175 light / 240 dark x power factor 25) | 4,375 / 6,000, plus riders and the aura | 4,000 (rank 36) to 8,000 (rank 1), by rank | `DamageScale.Kind.DJINN_WEAPON` |
| Djinn Extreme Magic (raw 11-34, relic clamp 400, then the power factor x25 for the bearer) | 400 x 25 = 10,000 per hit; dark x1.25 | 4,000 to 8,000 by rank | `DamageScale.Kind.EXTREME`, set around `DjinnMagic.hit` |
| Djinn Half Equip rider, bearer's other blows | rider 6 to 8 (players capped at 6); blows x25 | at most the Djinn's own band number | `DJINN_BEARER` |
| Bosses hitting players (mod bosses, `entity/` of the mod, balance law) | each boss's own numbers; PvP 40% cap | 10,000 | event ceiling for hits from mod entities |
| Aura (`AuraScaling`) | x1.6 up to the grade max | unchanged | already bounded; the ceiling follows |
| Late-game scaling (3.90.0) and EP scale | x6 and x12 | unchanged | multipliers kept, ceiling on top |

The Genesis relic weapons, old stat max vs new ceiling: Genesis band of `GradeDamage` 260-350 (Akashic 420-560) is the **stat** and is unchanged; what it can turn into after the multipliers is
what the ceiling limits. Weapons by Genesis place are in `docs/rankings.md`.

## 4. Djinn rank table (strongest first), weapon hit and Extreme Magic

Weapon hit = Extreme Magic per-hit ceiling = `4000 + 4000 x (36 - rank) / 35`. Grade by place: Genesis 1-3, God 4-12, Legend 13-24, Unique 25-36. The dark twin has the same rank.

| Rank | Djinn | Grade | Hit | Element | Extreme Magic damage type |
|---|---|---|---|---|---|
| 1 | baal | Genesis | 8,000 | lightning | `minecraft:lightning_bolt` |
| 2 | paimon | Genesis | 7,886 | wind | `anime_multiverse:djinn_wind` |
| 3 | valefor | Genesis | 7,771 | lightning | `minecraft:lightning_bolt` |
| 4 | zagan | God | 7,657 | life | `anime_multiverse:djinn_life` |
| 5 | vinea | God | 7,543 | ice | `minecraft:freeze` |
| 6 | amon | God | 7,429 | fire | `minecraft:in_fire` |
| 7 | focalor | God | 7,314 | wind | `anime_multiverse:djinn_wind` |
| 8 | furcas | God | 7,200 | wind | `anime_multiverse:djinn_wind` |
| 9 | zepar | God | 7,086 | force | `minecraft:player_explosion` |
| 10 | bifron | God | 6,971 | lightning | `minecraft:lightning_bolt` |
| 11 | marchosias | God | 6,857 | fire | `minecraft:in_fire` |
| 12 | furfur | God | 6,743 | sound | `minecraft:sonic_boom` |
| 13 | sitri | Legend | 6,629 | light | `anime_multiverse:djinn_light` |
| 14 | vassago | Legend | 6,514 | water | `anime_multiverse:djinn_water` |
| 15 | bael | Legend | 6,400 | force | `minecraft:player_explosion` |
| 16 | belial | Legend | 6,286 | fire | `minecraft:in_fire` |
| 17 | andrealphus | Legend | 6,171 | light | `anime_multiverse:djinn_light` |
| 18 | agares | Legend | 6,057 | water | `anime_multiverse:djinn_water` |
| 19 | barbatos | Legend | 5,943 | life | `anime_multiverse:djinn_life` |
| 20 | gusion | Legend | 5,829 | water | `anime_multiverse:djinn_water` |
| 21 | vepar | Legend | 5,714 | water | `anime_multiverse:djinn_water` |
| 22 | shax | Legend | 5,600 | sound | `minecraft:sonic_boom` |
| 23 | buer | Legend | 5,486 | life | `anime_multiverse:djinn_life` |
| 24 | kimaris | Legend | 5,371 | dark | `anime_multiverse:djinn_dark` |
| 25 | amdusias | Unique | 5,257 | sound | `minecraft:sonic_boom` |
| 26 | decarabia | Unique | 5,143 | light | `anime_multiverse:djinn_light` |
| 27 | seere | Unique | 5,029 | wind | `anime_multiverse:djinn_wind` |
| 28 | dantalion | Unique | 4,914 | light | `anime_multiverse:djinn_light` |
| 29 | andromalius | Unique | 4,800 | lightning | `minecraft:lightning_bolt` |
| 30 | glasya_labolas | Unique | 4,686 | force | `minecraft:player_explosion` |
| 31 | haagenti | Unique | 4,571 | wind | `anime_multiverse:djinn_wind` |
| 32 | crocell | Unique | 4,457 | ice | `minecraft:freeze` |
| 33 | balam | Unique | 4,343 | fire | `minecraft:in_fire` |
| 34 | alloces | Unique | 4,229 | fire | `minecraft:in_fire` |
| 35 | camio | Unique | 4,114 | dark | `anime_multiverse:djinn_dark` |
| 36 | murmur | Unique | 4,000 | sound | `minecraft:sonic_boom` |

Source of the order: the owner's list for ranks 1-8, my reading of Magi canon for the rest (the wiki was not reachable, see section 1).

## 5. Damage types of the Extreme Magic

| Concept | Damage type | Base-game interplay |
|---|---|---|
| fire | `minecraft:in_fire` (`is_fire`) | Fire Resistance, fire-immune mobs; a Heat Djinn bearer's calm (80%) |
| lightning | `minecraft:lightning_bolt` (`is_lightning`) | a Lightning Djinn bearer's calm (80%) |
| ice | `minecraft:freeze` (`is_freezing`, bypasses armour) | powder-snow immune mobs (strays, snow golems) |
| sound | `minecraft:sonic_boom` | a Sound Djinn bearer's calm (80%) |
| force | `minecraft:player_explosion` (`is_explosion`) | blast protection; a Strength Djinn bearer's calm (40%) |
| water | NEW `anime_multiverse:djinn_water`, tag `is_djinn_water` | a Water Djinn bearer's calm (80%) |
| wind | NEW `anime_multiverse:djinn_wind`, tag `is_djinn_wind` | a Wind Djinn bearer's calm (80%) |
| life | NEW `anime_multiverse:djinn_life`, tag `is_djinn_life` | a Life Djinn bearer's calm (40%, as the old magic type had) |
| light | NEW `anime_multiverse:djinn_light`, tag `is_djinn_light` | a Light Djinn bearer's calm (25%, as the old magic type had) |
| dark | NEW `anime_multiverse:djinn_dark` | none (nothing in the base game resists it) |

Data: `assets/anime_multiverse/djinn_extreme_types.json` (Djinn -> concept -> type), `data/anime_multiverse/damage_type/djinn_*.json` (exhaustion 0, no hunger drain, no difficulty scaling) and
`data/anime_multiverse/tags/damage_type/is_djinn_*.json`. The old types (fire, lightning, sonic, strength -> player attack, the rest magic) come back with `extremeElementTypes = false`.
Caveat: a fire-immune boss now takes no damage from Amon's Extreme Magic (that is the interplay asked for); the PvP and boss caps are unchanged.

## 6. Cost (worst case)

One `LivingIncomingDamageEvent` listener at LOWEST priority: a few `instanceof` checks, at most three registry-key lookups and one small record per damage event; no world scan, no tick work, no
packet, no entity, no allocation beyond that record. The rank line is built on the client's tooltip event only (a map lookup per hovered item). The Extreme Magic scope is one static
flag set around one `hurt` call (server thread). Worst case = the number of damage events the server already has.

## 7. Tests

Pure: `DamageScaleTest` (band maths, monotonic, ceilings, the 15,000 maximum with absurd settings, floor, off = old), `RankingsTest` (table loads, 36 Djinn once, tiers, Genesis weapons all
placed, broken table never throws, Extreme Magic table and data files), `DamageScaleSourceScanTest` (wiring). Server self-tests `dmgscale.djinn_band`, `dmgscale.cap`, `dmgscale.ranks`
(smoke prefix `dmgscale`). No existing test pinned an old damage number (no raw number was changed).

## 8. NEW / REPLACED / KEPT

| | |
|---|---|
| NEW | `balance/DamageScale`, `DamageScaleConfig`, `DamageScaleEvents`, `DamageScaleSelfTests`; `relics/Rankings`; `relics/djinn/DjinnExtremeTypes`; `tooltip/RankTooltip`; `assets/anime_multiverse/rankings.json` and `djinn_extreme_types.json`; 5 damage types and 4 tags; lang fragment `tools/lang/damage_scale.json`; the docs |
| REPLACED | the old Extreme Magic damage types (fire, lightning, sonic, magic) by the element's own type (switchable back) |
| KEPT | every formula and raw number, the pvp hit cap, the late-game and EP scaling, the power raise, the hero buff, the relic grade ranges, Elsdocia's own 400 |

## 9. Caveats (not seen in a game)

* The ceiling holds for hits that go through `LivingIncomingDamageEvent`; direct `setHealth` or `kill` paths of the mod (if any) are outside it.
* Raising the Extreme Magic to its rank number is not done (only a ceiling): the relic helper clamps the raw hit at 400 before the power factor, so without the power raise an Extreme Magic stays at
  its old small numbers.
* "Weapon hit" for a Djinn weapon is the swing on a mob; skills cast from it are ordinary spells under the 10,000 ceiling.
* The Vinea / Focalor / Furcas elements follow the owner's examples, not the mod's earlier affinity.
