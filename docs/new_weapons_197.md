# New weapons of 1.97.0: 13 Legend, 12 God and 10 Genesis grade weapons (wave 71)

Owner request: "add more weapons to the legend, God, and Genesis grade". 35 weapons were added over many weapon types and many worlds (Dungeons and Dragons artifacts, Black Clover, Tensura, Fairy Tail, The Rising of the Shield Hero, The Seven Deadly Sins, DanMachi, old myths), each a full relic of the Relics of Many Worlds (`docs/relic_format.md`). **Everything here is NEW: no existing weapon, model, skill, act or id was removed or changed** (CLAUDE.md rules 2 and 5; section 7 lists the additions to shared files). The table and the generators are the source: edit `tools/relics/new197_roster.py` and rerun the tools, not the generated files.

## 1. The weapons

Damage is the owner's table (`relics/GradeDamage`: normal 20-30, special 35-55, rare 60-85, unique 90-115, legend 130-175, god 190-240, genesis 260-350), the place inside the range chosen by the weapon type. Speed is attacks a second. A weapon's acts are the first (use, or sneak + use for a bow), the second (sneak + use; the worn-relic key for a bow) and the third (the worn-relic key). Cost is a share of the magicules (then aura, as every relic pays).

### Legend grade (13 weapons, damage 130-175)

| id | name | type (class) | damage / speed | passive | acts | Tensura engravings | road |
|---|---|---|---|---|---|---|---|
| `vorpal_sword` | Vorpal Sword (Dungeons and Dragons) | longsword (longsword) | 152.5 / 1.7 | 60% more damage against a foe under 30% of its health | Snicker-Snack (use, 30 s, 3%); Severing Arc (sneak + use, 12 s, 2%) | severance, barrier_piercing, swift | shadow |
| `holy_avenger` | Holy Avenger (Dungeons and Dragons) | longsword (longsword) | 152.5 / 1.5 | 60% more damage against the undead, demons and other fiends | Smite (use, 18 s, 3%); Sacred Oath (sneak + use, 90 s, 5%) | holy_weapon, magic_interference, magic_protection | holy_church |
| `luck_blade` | Luck Blade (Dungeons and Dragons) | short sword (shortsword) | 143.5 / 1.9 | 50% more damage on every blow (a 15% chance on each blow) | Fortune's Strike (use, 14 s, 2%); Lucky Break (sneak + use, 2 min, 4%) | swift, vigor, sturdy | feywild_grove |
| `frostbrand` | Frostbrand (Dungeons and Dragons) | sabre (sabre) | 146 / 1.7 | 15% more damage on every blow; slowness for 4 seconds on the foe | Rime Cleave (use, 14 s, 3%); Winter's Grip (sneak + use, 70 s, 5%) | elemental_boost, enervation, sturdy | frost |
| `dragon_slayer` | Dragon Slayer (Dungeons and Dragons) | greatsword (greatsword) | 175 / 1 | 100% more damage against dragons, wyrms, serpents and hydras | Wyrm Cleave (use, 22 s, 3%); Dragon's Bane (sneak + use, 75 s, 5%) | severance, crushing, sturdy | infernal_foundry |
| `sunsword` | Sunsword (Dungeons and Dragons) | longsword (longsword) | 152.5 / 1.6 | 40% more damage in open daylight | Sunbeam (use, 25 s, 3%); Dawn Burst (sneak + use, 80 s, 5%) | holy_weapon, elemental_boost, magic_weapon | radiant |
| `crimson_lions_flame_lance` | Crimson Lion's Flame Lance (Black Clover) | spear (spear) | 155 / 1.3 | 15% more damage on every blow; sets the foe alight for 5 seconds | Lion Thrust (use, 20 s, 3%); Salamander Rise (sneak + use, 75 s, 5%) | elemental_boost, severance, sturdy | flame |
| `silver_eagles_galewing_bow` | Silver Eagle's Galewing Bow (Black Clover) | bow (bow) | 152.5 / 1.3 | 60% more damage against a foe in the air | Gale Volley (sneak + use, 30 s, 3%); Eagle Dive (the worn-relic key, 60 s, 4%) | swift, barrier_piercing, elemental_boost | sky_citadel |
| `starlight_whip` | Starlight Whip (Fairy Tail) | whip (whip) | 139 / 1.5 | 40% more damage at night | Star Lash (use, 15 s, 2%); Constellation Bind (sneak + use, 75 s, 4%) | swift, magic_weapon, enervation | astral |
| `fairy_heart_gauntlets` | Fairy Heart Gauntlets (Fairy Tail) | pair of gauntlets (dagger) | 155 / 1.8 | 10% more damage on every blow; 5% of the damage comes back to you as life | Fairy Smash (use, 18 s, 3%); Friends' Rally (sneak + use, 2 min, 5%) | crushing, vitality, sturdy | celestial_court |
| `wavebreaker_chakram` | Wavebreaker Chakram (The Rising of the Shield Hero) | chakram (handaxe) | 148 / 1.8 | 50% more damage against a foe in water or rain | Spinning Cut (use, 20 s, 3%); Tidal Return (sneak + use, 70 s, 4%) | severance, elemental_boost, swift | sunken_temple |
| `noon_cleaver` | Noon Cleaver (The Seven Deadly Sins) | great axe (greataxe) | 170.5 / 0.9 | 50% more damage while you are at full health | Zenith Cleave (use, 27 s, 4%); Sunrise Wall (sneak + use, 90 s, 5%) | crushing, severance, vigor | mountain_monastery |
| `calamity_scythe` | Calamity Scythe (That Time I Got Reincarnated as a Slime) | scythe (scythe) | 170.5 / 1.1 | 40% more damage against a foe under 30% of its health; 5% of the damage comes back to you as life | Soul Reap (use, 30 s, 4%); Lord's Decree (sneak + use, 2 min, 6%) | soul_eater, ruination, enervation | necropolis |

### God grade (12 weapons, damage 190-240)

| id | name | type (class) | damage / speed | passive | acts | Tensura engravings | road |
|---|---|---|---|---|---|---|---|
| `staff_of_the_magi` | Staff of the Magi (Dungeons and Dragons) | staff (staff) | 212.5 / 1.4 | 20% more damage on every blow | Retributive Strike (use, 40 s, 6%); Spell Turning (sneak + use, 100 s, 8%); Arcane Tempest (the worn-relic key, 4 min, 12%, 3/day) | magic_interference L3, magicule_absorption, magic_capacity, magic_weapon | cursed_archive |
| `hammer_of_thunderbolts` | Hammer of Thunderbolts (Dungeons and Dragons) | hammer (hammer) | 227.5 / 0.9 | 60% more damage against a foe in water or rain (a 50% chance on each blow); weakness for 6 seconds on the foe against a foe in water or rain | Thunderclap (use, 40 s, 6%); Hurled Bolt (sneak + use, 70 s, 8%); Tempest (the worn-relic key, 4 min, 12%, 3/day) | crushing L3, elemental_boost, sturdy, barrier_piercing | storm |
| `kusanagi_the_grass_cutter` | Kusanagi, the Grass-Cutter (Japanese myth) | katana (katana) | 212.5 / 1.8 | 70% more damage against dragons, wyrms, serpents and hydras | Wind Cut (use, 20 s, 5%); Cloud Step (sneak + use, 55 s, 7%); Eightfold Cut (the worn-relic key, 4 min, 10%, 3/day) | severance L3, swift, barrier_piercing, elemental_boost | verdant |
| `gungnir_the_unerring` | Gungnir, the Unerring (Norse myth) | spear (spear) | 217.5 / 1.3 | 20% more damage on every blow; glowing for 10 seconds on the foe | Unerring Cast (use, 35 s, 7%); Allfather's Pledge (sneak + use, 2 min, 8%); Valhalla Rain (the worn-relic key, 4 min, 12%, 3/day) | barrier_piercing L3, swift, magic_interference, holy_weapon | sky_citadel |
| `leviathans_crown_trident` | Leviathan's Crown Trident (Greek myth) | trident (trident) | 217.5 / 1.3 | 60% more damage against a foe in water or rain; slowness for 5 seconds on the foe against a foe in water or rain | Tidal Lance (use, 30 s, 6%); Maelstrom (sneak + use, 90 s, 8%); Sea King's Wrath (the worn-relic key, 4 min, 12%, 3/day) | elemental_boost L3, barrier_piercing, magic_weapon, sturdy | drowned_crypt |
| `wizard_kings_vow` | Wizard King's Vow (Black Clover) | rapier (rapier) | 195 / 2 | 35% more damage against a foe with 100 health or more | Clover Thrust (use, 18 s, 6%); Magic Zone (sneak + use, 100 s, 8%); King's Judgement (the worn-relic key, 4 min, 12%, 3/day) | swift L3, magic_weapon, barrier_piercing, magic_capacity | verdant |
| `wand_of_the_wisdom_king` | Wand of the Wisdom King (That Time I Got Reincarnated as a Slime) | wand (wand) | 192.5 / 1.8 | 40% more damage against a boss | Sagely Barrage (use, 20 s, 6%); Absolute Defence (sneak + use, 100 s, 8%); Accelerated Thought (the worn-relic key, 3 min, 10%, 3/day) | magic_weapon L3, magic_capacity, magicule_absorption, magic_protection | cursed_archive |
| `dragon_force_twin_fangs` | Dragon Force Twin Fangs (Fairy Tail) | pair of fang-daggers (dagger) | 190 / 2.4 | 80% more damage while you sneak | Twin Fang Flurry (use, 10 s, 5%); Dragon Force (sneak + use, 100 s, 8%); Dragon's Roar (the worn-relic key, 4 min, 10%, 3/day) | severance L3, swift, elemental_boost, vitality | flame |
| `calamity_wave_aegis` | Calamity Wave Aegis (The Rising of the Shield Hero) | shield (shield) | 200 / 1.2 | 10% more damage on every blow; slowness for 3 seconds on the foe | Wave Guard (use, 45 s, 6%); Retaliating Surge (sneak + use, 80 s, 8%); Aegis Anthem (the worn-relic key, 4 min, 10%, 3/day) | sturdy L3, magic_protection, severance_protection, holy_coat | sunken_temple |
| `hellblaze_the_demon_sin` | Hellblaze, the Demon Sin (The Seven Deadly Sins) | longsword (longsword) | 215 / 1.6 | 80% more damage while you are under 40% of your health; 4% of the damage comes back to you as life | Hellblaze Slash (use, 22 s, 6%); Demon Mark (sneak + use, 100 s, 8%); Black Inferno (the worn-relic key, 4 min, 12%, 3/day) | elemental_boost L3, severance, ruination, energy_steal | infernal_foundry |
| `minotaur_kings_labrys` | Minotaur King's Labrys (DanMachi) | double axe (battleaxe) | 225 / 1 | 50% more damage against a foe with 100 health or more | Labrys Cleave (use, 24 s, 6%); Bull Charge (sneak + use, 60 s, 8%); King's Roar (the worn-relic key, 4 min, 10%, 3/day) | crushing L3, severance, sturdy, vigor | dwarven_forgehold |
| `dawnpiercer_crossbow` | Dawnpiercer Crossbow (original) | crossbow (crossbow) | 217.5 / 1.5 | 30% more damage in open daylight | Piercing Bolt (sneak + use, 25 s, 6%); Sunrise Volley (the worn-relic key, 60 s, 8%); Dawn's Net (the worn-relic key, 3 min, 10%, 3/day) | barrier_piercing L3, swift, magic_weapon, holy_weapon | radiant |

### Genesis grade (10 weapons, damage 260-350)

| id | name | type (class) | damage / speed | passive | acts | Tensura engravings | road |
|---|---|---|---|---|---|---|---|
| `blackrazor` | Blackrazor (Dungeons and Dragons) | greatsword (greatsword) | 350 / 1 | 20% more damage on every blow; 8% of the damage comes back to you as life | Devour Soul (use, 25 s, 8%); Void Cleave (sneak + use, 60 s, 10%); Hunger Eternal (the worn-relic key, 5 min, 14%, 2/day) | soul_eater L4, ruination, energy_steal, enervation, severance | shadow |
| `gae_bolg_heartseeker` | Gae Bolg, Heart-Seeker (Celtic myth) | spear (spear) | 309.5 / 1.3 | 90% more damage against a foe under 30% of its health; wither for 5 seconds on the foe against a foe under 30% of its health | Heart Thrust (use, 30 s, 10%); Thirty Barbs (sneak + use, 70 s, 12%); Wound That Will Not Close (the worn-relic key, 5 min, 14%, 2/day) | barrier_piercing, enervation, severance, soul_eater, energy_steal | blood |
| `starforged_aeon_bow` | Starforged Aeon Bow (original) | bow (bow) | 305 / 1.3 | 35% more damage against a boss | Meteor Arrow (sneak + use, 40 s, 10%); Constellation Volley (the worn-relic key, 90 s, 12%); Starfall (the worn-relic key, 5 min, 14%, 2/day) | barrier_piercing L4, swift, magic_weapon, elemental_boost, transcendence | astral |
| `primordial_hearth_blade` | Primordial Hearth Blade (DanMachi) | dagger (dagger) | 260 / 2.4 | 70% more damage while you are under 40% of your health; 5% of the damage comes back to you as life | Falna Flurry (use, 10 s, 8%); Hearth Dash (sneak + use, 40 s, 10%); Goddess's Blessing (the worn-relic key, 5 min, 14%, 2/day) | vitality L4, swift, restoration, severance, growth | celestial_court |
| `void_origin_katana` | Void Origin Katana (That Time I Got Reincarnated as a Slime) | katana (katana) | 300.5 / 1.8 | 50% more damage against the undead, demons and other fiends | Dimension Cut (use, 25 s, 8%); Void Step (sneak + use, 45 s, 10%); World Sever (the worn-relic key, 5 min, 14%, 2/day) | severance L4, barrier_piercing, magic_interference, transcendence, intangibility | abyss |
| `fairy_glitter_gauntlets` | Fairy Glitter Gauntlets (Fairy Tail) | pair of gauntlets (dagger) | 309.5 / 1.8 | 30% more damage on every blow; 5% of the damage comes back to you as life | Glitter Barrage (use, 25 s, 8%); Fairy Glitter (sneak + use, 90 s, 12%); Law of the Guild (the worn-relic key, 5 min, 14%, 2/day) | magic_weapon L4, crushing, magic_protection, vitality, restoration | celestial_court |
| `cardinal_halo_chakram` | Cardinal Halo Chakram (The Rising of the Shield Hero) | chakram (handaxe) | 296 / 1.8 | 50% more damage while you are falling | Halo Throw (use, 20 s, 8%); Judgement Ring (sneak + use, 80 s, 10%); Cardinal Descent (the worn-relic key, 5 min, 14%, 2/day) | severance L4, elemental_boost, swift, holy_weapon, barrier_piercing | radiant |
| `magic_emperors_black_halberd` | Magic Emperor's Black Halberd (Black Clover) | halberd (glaive) | 332 / 1 | 60% more damage against a foe with 100 health or more | Emperor's Sweep (use, 28 s, 10%); Dimensional Rift (sneak + use, 90 s, 12%); Dark Triad (the worn-relic key, 5 min, 14%, 2/day) | barrier_piercing L4, severance, magic_interference, magicule_absorption, ruination | spatial |
| `starsplitter_hammer` | Starsplitter Hammer (The Seven Deadly Sins) | hammer (hammer) | 327.5 / 0.9 | 60% more damage at night | Meteor Smash (use, 30 s, 10%); Gravity Well (sneak + use, 90 s, 12%); Starsplit (the worn-relic key, 5 min, 14%, 2/day) | crushing L4, sturdy, elemental_boost, severance, vigor | gravity |
| `sword_of_the_first_dawn` | Sword of the First Dawn (original) | longsword (longsword) | 305 / 1.5 | 70% more damage in open daylight | Dawnlight Cut (use, 22 s, 8%); First Light (sneak + use, 60 s, 10%); Daybreak (the worn-relic key, 5 min, 14%, 2/day) | holy_weapon L4, severance, magic_protection, elemental_boost, holy_coat | holy_church |

## 2. What each weapon looks like (3D model in hand, built by `tools/relics/new197_*.py`)

* **Vorpal Sword** (`vorpal_sword`): A thin, long blade with a pale fuller and a dark pommel eye; the edge shimmers where it is sharpest.
* **Holy Avenger** (`holy_avenger`): A crusader longsword with a winged crossguard, a golden sun disc pommel and a white-gold blade.
* **Luck Blade** (`luck_blade`): A short, broad leaf-shaped blade of green steel; the guard is a gold four-leaf clover.
* **Frostbrand** (`frostbrand`): A curved ice-blue sabre with a rime-white edge, a snowflake guard and a blue-leather grip.
* **Dragon Slayer** (`dragon_slayer`): A huge, notched greatsword: a wide dark blade with a crimson fuller, tooth-like nicks and a spiked red crossguard.
* **Sunsword** (`sunsword`): A golden hilt with a blade of solid sunlight: a bright core, a long yellow glow and sun-ray guards.
* **Crimson Lion's Flame Lance** (`crimson_lions_flame_lance`): A long red lance with a golden lion-head socket, a flame-shaped leaf blade and a trailing mane of red cloth.
* **Silver Eagle's Galewing Bow** (`silver_eagles_galewing_bow`): A recurve bow whose limbs are spread silver feathers over a green glass core, with a blue wind-gem grip.
* **Starlight Whip** (`starlight_whip`): A long coiled whip of braided silver-violet light with a gold key handle and a star at the tip.
* **Fairy Heart Gauntlets** (`fairy_heart_gauntlets`): A pair of broad red-and-gold gauntlets with a heart crest on each back and winged knuckle guards.
* **Wavebreaker Chakram** (`wavebreaker_chakram`): A large ring blade of blue steel with a rolling-wave edge pattern, a white inner rim and a leather hand-loop.
* **Noon Cleaver** (`noon_cleaver`): A great two-bladed golden axe with a sun disc on each head, a dark-wood haft and gold-bound grip.
* **Calamity Scythe** (`calamity_scythe`): A tall black pole with a huge violet crescent blade, bone-white skull ferrules and a glowing soul crystal.
* **Staff of the Magi** (`staff_of_the_magi`): A tall twisted violet staff ringed in gold with a floating crystal at the head and arcane runes down the shaft.
* **Hammer of Thunderbolts** (`hammer_of_thunderbolts`): A massive hammer: a blue-grey steel head with gold bands and lightning engraving on an iron haft with a lanyard ring.
* **Kusanagi, the Grass-Cutter** (`kusanagi_the_grass_cutter`): A long, slightly curved jade-green blade in a white-and-green wrapped hilt with a cloud-pattern guard.
* **Gungnir, the Unerring** (`gungnir_the_unerring`): A long golden spear with a leaf-shaped head carved with runes, a blue-wrapped grip and a spiral-wound shaft.
* **Leviathan's Crown Trident** (`leviathans_crown_trident`): A tall trident of blue steel with a coral crown of prongs, wave-etched barbs and a pearl at the socket.
* **Wizard King's Vow** (`wizard_kings_vow`): A slim white rapier with a green-and-gold four-leaf clover guard, a slender needle blade and a mana-gem pommel.
* **Wand of the Wisdom King** (`wand_of_the_wisdom_king`): A slim wand of pale blue crystal ringed with violet and silver, with a luminous core crystal at the tip.
* **Dragon Force Twin Fangs** (`dragon_force_twin_fangs`): A curved white fang-blade with a red-bound grip, a ring pommel and ember veins along the spine.
* **Calamity Wave Aegis** (`calamity_wave_aegis`): A heavy kite shield: sea-blue steel with a gold wave-crest border, a central sapphire boss and a studded rim.
* **Hellblaze, the Demon Sin** (`hellblaze_the_demon_sin`): A black broadsword with a jagged, torn edge, lava-red veins in the fuller and a horned demon guard.
* **Minotaur King's Labrys** (`minotaur_kings_labrys`): A huge double-headed battle axe, dark iron on a bone-and-leather haft, with horned crescents and glowing ember cracks.
* **Dawnpiercer Crossbow** (`dawnpiercer_crossbow`): A heavy crossbow: dark-wood stock, a gold sun-wing prod, a steel latch and a glowing sunstone on the rail.
* **Blackrazor** (`blackrazor`): A black glass greatsword with a wide, smooth blade, violet inner light, a skull-eye guard and a chain-wrapped grip.
* **Gae Bolg, Heart-Seeker** (`gae_bolg_heartseeker`): A dark crimson spear with a barbed, thorn-covered head, a blood-red sinew wrap and a glowing red core.
* **Starforged Aeon Bow** (`starforged_aeon_bow`): A huge white-gold bow: crescent limbs set with stars, a comet-tail string, a glowing blue star core.
* **Primordial Hearth Blade** (`primordial_hearth_blade`): A short black dagger with a golden flame-pattern blade, a glowing hearth core in the guard and a red-wrapped grip.
* **Void Origin Katana** (`void_origin_katana`): A long blue-black katana with a cyan void-line hamon, a black-and-cyan cord grip and a ring-shaped tsuba.
* **Fairy Glitter Gauntlets** (`fairy_glitter_gauntlets`): White-gold gauntlets with winged knuckles, a glowing star-gem back plate and fairy-wing flares on the forearm.
* **Cardinal Halo Chakram** (`cardinal_halo_chakram`): A broad golden ring blade with a halo of sea-glass studs, a cyan inner glow and a cross-bar hand grip.
* **Magic Emperor's Black Halberd** (`magic_emperors_black_halberd`): A long black halberd: an axe-blade and a spear tip in dark steel with magenta edges, a spiked clover guard and a gem socket.
* **Starsplitter Hammer** (`starsplitter_hammer`): A colossal bronze-gold hammer with star-cracked faces, a violet band and rune plates on an iron haft.
* **Sword of the First Dawn** (`sword_of_the_first_dawn`): A long white-gold sword whose blade is a bar of dawnlight in a gold frame, a sunburst guard and a white-leather grip.

Every weapon is a real baked mesh (8 to 17 named parts, 700 to 3200 quads) with a painted skin and a second additive glow skin; the Genesis weapons that carry a void part (Blackrazor, the Void Origin Katana, the Magic Emperor's Black Halberd, the Starsplitter Hammer) draw the dimensional void of the Genesis Demon-Slayer through the metal. The inventory icon is a new 16 x 16 sprite rendered from the finished mesh (outline, half-strength glow). The two bows and the crossbow have their draw states (`_pull1..3`, `_charged`); the bows lie on vanilla's diagonal with vanilla's display (`bow_vanilla.py`); the shield is placed and held like the Aegis of the Abyss. Previews: `docs/previews/new197/` (`sprites.png`, `models_LEGEND.png`, `models_GOD.png`, `models_GENESIS.png`, `held_view_sample.png`).

## 3. How the abilities work

Each act is one SHAPE of `Wave71Acts` (a cone cleave, a piercing line, a nova, a blink strike, an execution, a chain, a ward, a rally, a drain, a pull, a volley, a rush, a storm, a brand, a self buff). Every body runs inside `SafeCast.tick` (a fault is logged once, never thrown into the server tick), takes its foes only from `Fx.foesAround` and `Wave20Util` (hostile creatures, never allies, pets, summons, creative or spectator players), deals every blow through `Fx.damage` / `Fx.damageHeavy` (`BalanceLaw`: EP scaling, the cap on players, the shortened effect on bosses), clamps every radius, count and duration, and draws its effect with one of the mod's `VfxShape`s plus Rukh particles. An act's damage is a multiple of the weapon's own base damage (`DMG_<ID> * k`), so it follows the owner's table. Costs and cooldowns are the engine's (`Act.cooldown`, `.mana`, `.usesPerDay`, tiers and attunement as for every relic).

The passive of each weapon is one `Wave71Rules.Rule` (`Wave71Passives`, applied by `Wave71Events` on the blow being dealt, melee for a melee weapon and the loosed arrow or bolt for a bow or crossbow): a damage bonus under a condition (a foe low on health, an undead or fiend, a dragon, a flier, a wet foe, daylight, night, a full-health or wounded wielder, a boss, sneaking, falling, a big foe), an optional chance, a status on the foe, life stolen (capped) and a burn. The standing passives (attributes, resistances, lent Tensura skills) are the ordinary `Passive` kinds.

## 4. Registration, loot and Better Combat

* Registered by `relics/wave/Wave71` (found by `WaveRegistry`, which scans waves 8 to 80): the item, the creative tab entry, `/multiverse relic give <id>`, the tooltip, the Curios and attunement rules, the Djinn vessel flag, the forge and the sockets are the core's, as for every relic.
* Loot is by grade through the existing relic pools, so the new weapons enter every source that rolls their grade: the `nusmp:relic` entries of the vault tables (`chests/dungeon_relic_vault`, `chests/labyrinth_pedestal`: Legend to God at weight 3 / 15, God to Genesis at 1 / 4), the Crucible of Choice of the Mega-Dungeon (unique to genesis offers) and the God of Order's ledger (one of each in a world). Each weapon is also placed on a dungeon road in `data/nusmp/mega/relic_themes.json` (the theme column), so taking it in a Crucible leads to a fitting next region.
* Better Combat: a file in `data/nusmp/weapon_attributes/<id>.json` for every melee weapon (31; the two bows, the crossbow and the shield have none, as every ranged weapon and shield), made by `python3 -B tools/bettercombat/inventory.py` and `gen_attributes.py`; the gauntlets are class `dagger` (their size and swing) with the damage of a club.
* Size classes: a row for every id in `assets/nusmp/weapon_classes.json` and `tools/relics/weapon_classes.json`; the first-person display of the long weapons is the oversize rule (`new197_presets.py`).

## 5. Tests

JUnit (all pure, run without the game): `relics/wave/Wave71DefsTest` (counts, unique ids, 3D, engravings from the Tensura list, 2 or 3 acts, damage inside the grade's range, ranged marks, the lang fragment), `Wave71RulesTest` (the rule arithmetic and the table of 35 rules), `Wave71AssetsTest` (mesh, skins, 16 x 16 sprite, models, draw states, class rows, Better Combat files, dungeon roads); the existing `WeaponMeshesTest`, `BowDisplayGuardTest`, `WeaponClassesTest`, `WeaponAttributesTest`, `RelicHandCoverageTest`, `RelicRenderCoverageTest` and `RelicThemesDataTest` pass with the new weapons. The in-game self-test `relic.new197` (registered by `Wave71.hooks`, run by the CI server smoke test) checks the registry (grades, 3D, engravings, acts, passive rules, damage range, the files in the jar).

## 6. Tools

```
python3 -B tools/relics/new197_gen.py        # Wave71.java, Wave71Passives.java, tools/lang/new_weapons_197.json, class rows, dungeon roads, this page's table
python3 -B tools/relics/new197_build.py      # all 35 meshes, skins, sprites, item models, then bow_vanilla.py and new197_presets.py
python3 -B tools/relics/new197_audit.py      # the size / orientation audit of the 35
python3 -B tools/relics/new197_sheet.py out.png   # the sprites; --meshes out.png [id | grade:GOD]  the models
python3 -B tools/bettercombat/inventory.py && python3 -B tools/bettercombat/gen_attributes.py
python3 -B tools/item_preview/preview_weapons.py vorpal_sword blackrazor   # the held view, build/item_preview/weapons.png
```

## 7. Edits to files that already existed (all additions)

* `assets/nusmp/weapon_classes.json`, `tools/relics/weapon_classes.json`: 35 class rows appended.
* `data/nusmp/mega/relic_themes.json`: 35 dungeon-road rows appended.
* `tools/bettercombat/manifest.json`: the 31 melee weapons added (regenerated by the tool).
* `tools/relics/display_presets_manifest.json`: the rows of the 26 long weapons added.
* New: `relics/GradeDamage.java` (a STAND-IN with exactly `range(String)` and `damage(String, String)`; replace it with the full damage-table class when that is merged), `relics/wave/Wave71*.java`, three test classes, `tools/lang/new_weapons_197.json`, 31 Better Combat files, the assets, `tools/relics/new197_*.py`, this page and the previews.
* Not touched, as asked: the version, the changelog, the README, `en_us.json` (the lead merges `tools/lang/new_weapons_197.json`), the Time Magic VFX.

## 8. What could not be verified

Nothing can be run in a game here: the models, skins, sprites, glow and void layers, the draw states, the held pose and size of each weapon, the particle and VFX shapes of the acts, the act numbers against a real boss (the balance law scales them), the passive hooks and the loot rolls are all unseen in the game. The previews are a software rasteriser, a re-implementation of the renderer (the void is shown as a crimson star field). The Java was type-checked against the API stubs (no real compile) and the JUnit tests ran in a shim without Minecraft. The Better Combat files use the presets of the class (the swings are the parent's own). The two bows and the crossbow follow the proven pipeline but their draw poses are unseen.
