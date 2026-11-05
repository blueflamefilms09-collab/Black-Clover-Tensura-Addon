# Beast Will skills (3.58.0)

Every Beast Will (the nine of 3.45.0 and the eighteen legendary and mythic ones) is now also a **pair of Tensura skills**: a **Unique** skill (`bw_<key>_unique`) that evolves into an **Ultimate** (`bw_<key>`) once mastered. Same pattern as the Dragon Ball skills (3.56.0, `docs/dragonball_skills.md`). 27 beasts, 54 skills. The key is the will id without `_will` (`lion_will` -> `bw_lion`, `bw_lion_unique`). Skill ids are static (skills register before the datapacks load), so the table is fixed in `skill/rules/BeastSkillRules.java`; a will whose data file is missing still has its skills.

## How it works

* **Independent of the will.** The skills do not need the will to be bound, active or assimilated, and they change nothing of the will system (binding, assimilation, forms, strain, drops: all KEPT). They are ordinary Magicule-costed Tensura skills.
* **Obtain:**
  * master the Beast Will itself (mastery 100 %): the Unique of that beast is granted once (a one-time flag per beast on the player, nothing if the player already owns the Unique or the Ultimate; server setting `beastSkillGrantOnMastery`, default true). The hook is one line in `BeastWillManager.second` (`BeastSkills.grantOnMastery`);
  * `/multiverse beastskill give <player> <id> [unique|ultimate]` (permission 2; `<id>` is `bw_<key>`, `bw_<key>_unique`, the key or the will id) and `/multiverse beastskill list`;
  * `/tensura edit <player> ability grant nusmp:bw_<key>_unique`.
* **Evolution:** a MASTERED Unique plus the server's minimum EP (`EVOLUTION_MIN_EP`) turns into the Ultimate, checked in the same 5-second player check as every other pair (`SkillEvolution` calls `BeastSkills.evolve` right after `DbSkills.evolve`; the older evolution table is untouched and still has exactly seven entries).
* **Passives** sit under the skill's one ON/OFF toggle. The Unique form has only the first passive at half strength; the Ultimate has all of them.
* **Actives** are on the mode wheel. "(crouch)" modes need sneaking. The Unique wheel is the first two actives at half strength; the Ultimate wheel has four actives plus **one mastery-only finisher** that is unique to the beast and answers only a mastered skill (cheaper and faster once mastered).
* **Scaling:** every beast has its own power scale. The seven legendary beasts hold the seven highest: Dragon 1.00, Typhon 1.00, Yamata no Orochi 0.98, Apophis 0.97, Jormungandr 0.96, Fenrir 0.95, Nidhoggr 0.94; then Kraken 0.88, Hydra 0.84, Cerberus 0.82, Hafgufa 0.80, Scylla and Charybdis 0.80, Sphinx 0.78, Kitsune 0.76, Ammit 0.76, Nue 0.74, Minotaur 0.72, Thunder Fang Chimera 0.72, Pegasus 0.70, Lion 0.68, Shadow Panther 0.66, Flame Serpent 0.66, Elderwood Guardian 0.64, Cu Sith 0.62, Wind Skimmer 0.54, Crested Boar 0.52, Ridge Wolf 0.50. A Unique is half an Ultimate (`UNIQUE_FACTOR` 0.5). Damage then goes through the Balance Law and the boss cap like every other skill (`SkillDamage`).
* **Settings (server):** `beastSkillsEnabled` (default true; false = the skills stay registered but every mode refuses, the passives do nothing and they do not evolve) and `beastSkillGrantOnMastery` (default true).
* **Visuals:** the actives reuse the Dragon Ball dispatcher's effects (`SkillFx` rings, beams and bursts, `VfxSpawn` shapes through the existing effect budget). Each finisher adds one `VfxShape` of its own (the `Finisher look` of its row: L = a line to the aim, G = on the ground at the aim, F = a gate or maw in front, S = round the caster). The Beast Will's own ability visuals (`BeastWillAbilities`) are NOT called: their `run` needs the datapack will definition and applies the will's own damage, strain and cooldown rules, so the skills use the shared `SkillFx` shapes instead. The skill icons are `textures/skill/bw_<key>.png` (gold frame, Ultimate) and `bw_<key>_unique.png` (silver frame, Unique), drawn by `tools/gen_bw_skill_icons.py`.
* **Code:** `BeastSkillRules` (pure table), `BeastSkill` (one class for both forms), `BeastMoves` (guard, veil, mend and mark are its own; beam, blast, stop, rush, barrage, disc, seal and psychic are `DbMoves`, shared code), `BeastPassives` (a second-by-second pass plus one damage listener), `BeastSkills` (registration, evolution, grant, command), self-test `skill.beast`, tests `BeastSkillRulesTest`.

Passive kinds: swift (speed), regeneration, armor, stealth, venom / blight (poison or wither aura), flight (slow fall, jump), illusion (dodge), frost (slowing aura), fury (low-health surge), bond (speed for allies), tide (water breathing, dolphin's grace), might (attack damage), sense (glowing foes), scorch (fire aura). Active moves: beam, blast, stop (a wall of wind that clears projectiles), rush, barrage, disc, seal (lock), psychic (hold, strike), guard, veil, mend, mark.

## Fenrir (God of War)

`bw_fenrir` / `bw_fenrir_unique` are themed on **Fenrir from God of War**, as the owner asked: Atreus's loyal pet wolf whose soul (pale blue-white light) was sealed into the giant wolf Garm and who became the giant Fenrir, his soul carried in Atreus's (Loki's) knife. Passives: Soul-Light (mending), Frostbound Coat (frost aura), Loyal Guardian (the protector wolf shields allies' pace). Actives: Call of the Spirit Wolf, Soul-Frost Breath, Soul Seal, Garm's Pounce, finisher **Soul of the Wolf, Unleashed** (a soul-bound giant-wolf strike). **The move details are themed on that lore and are NOT verified against the game.** Sources: web search snippets of godofwar.fandom.com and svg.com (owner's brief); the English wiki pages themselves were not fetched.

## NEW / REPLACED / KEPT

* **NEW:** all 54 skills, `BeastSkillRules`, `BeastSkill`, `BeastMoves`, `BeastPassives`, `BeastSkills`, `BeastSkillSelfTests` (`skill.beast`, added to the smoke list), `BeastSkillRulesTest`, `tools/gen_bw_skill_icons.py`, 54 icons, `tools/lang/beastskills.json`, the settings `beastSkillsEnabled` and `beastSkillGrantOnMastery`, the command `/multiverse beastskill`.
* **REPLACED:** nothing.
* **KEPT:** the Beast Will system (data files, binding, assimilation, forms, abilities, aura and look code), the Dragon Ball skills, the evolution table (seven entries), every older skill, id and icon. Shared files only appended to: `NUSkills` (one registration line), `SkillEvolution` (one call), `NUSMP` (two lines), `MultiverseCommands` (one line), `NUConfig` (two settings), `BeastWillManager` (one hook line), `smoke.yml` (`skill.beast`).

## Cost (rule 11), worst case

* **A press:** the same bounds as the Dragon Ball skills: one entity query per area, at most 24 creatures hit, at most 24 projectiles removed, a barrage is at most 24 hits on one target, one beam (at most 25 effect samples), one ring or burst, one `VfxShape` from the move, one more from the finisher, the cast circle and burst (two shapes). No spawned entities, no terrain edits, no chunk loads. Guard, veil, mend and mark touch at most 24 (12 for veil) creatures with timed effects.
* **Passives:** once a second per owner whose skill is ON (nothing when it is off): at most one query of 24 creatures per aura passive, effects on at most 8 foes (16 for sense) or 8 allies per passive, so at most about 40 `addEffect` calls a second per owner with the Ultimate at its worst; attribute modifiers are written only when the number changes. The damage listener returns at once unless the victim is a player with the form tag.
* **Network:** no packets of its own; effect packets go through the existing perf budgets (`SkillFx`, `VfxSpawn`). About 4 effect packets per press, 6 for a finisher.
* **State:** the player's own persistent tag (`nusmp_bws_form`, `nusmp_bws_ult`, `nusmp_bws_g_<key>`) and transient attribute modifiers, removed when the skill is toggled off, forgotten or disabled by the setting. Nothing is shared, nothing is keyed by a static "current player".

## Not verified

Nothing was run in a game or with gradle in this session: the Java was written against the signatures of the Dragon Ball files (which compile in CI) and the pure classes were compiled and tested with plain `javac`. The icons were drawn and checked by script only. Monster lore (all beasts except Fenrir) is general mythology and is not copied from a wiki.

## The 27 beasts
(Generated from the table; the numbers are the base values before the beast's scale. Costs and cooldowns are the un-mastered ones unless a mastered value is shown.)

### Dragon Sovereign, Wrath of the Mana Dragon (`bw_dragon`) and Will of the Dragon (`bw_dragon_unique`)

Will: `dragon_will`. Dragon: the strongest of the beast wills, legendary tier. Power scale 1.0 (Unique 0.5). Finisher look: `G:FIRE_PILLAR`.

| Passive (Ultimate: all; Unique: the first) | Effect |
|---|---|
| Draconic Hide | armor +6 and knockback resistance |
| Dragon Sight | glowing on up to 16 foes within 28 blocks |
| Draconic Vigor | heals 1.5% max health a second, cleanses every 5 s |

| Active | Where | Move | Cost (Magicule) | Cooldown (s) |
|---|---|---|---|---|
| Draconic Pulse | Unique + Ultimate | blast, dmg 60, radius 9.0, n 24 | 2000 | 10 |
| Dragon Breath | Unique + Ultimate | beam, dmg 85, range 52, radius 2.0, n 12, lock 40t | 3000 | 12 |
| Wing Gale (crouch) | Ultimate | stop, dmg 40, range 28, radius 4.0, n 24, lock 30t | 2600 | 14 |
| Tail Crush | Ultimate | rush, dmg 50, range 18, n 2, lock 20t | 3400 | 11 |
| Dragon King's Annihilation | mastery only | beam, dmg 150, range 70, radius 4.0, n 24, lock 60t | 28000 / 18666 mastered | 130 / 71 |

### Panther Sovereign, Prowler of the Moonless Night (`bw_shadow_panther`) and Will of the Shadow Panther (`bw_shadow_panther_unique`)

Will: `shadow_panther_will`. Shadow Panther: a swift stalker, rare tier. Power scale 0.66 (Unique 0.33). Finisher look: `G:SHADOW_POOL`.

| Passive (Ultimate: all; Unique: the first) | Effect |
|---|---|
| Night Stride | move speed +25% (attack speed half of that) |
| Shadow Cloak | invisible while crouching and unhurt |
| Predator's Grace | dodges 20% of melee hits (80% of projectiles) |

| Active | Where | Move | Cost (Magicule) | Cooldown (s) |
|---|---|---|---|---|
| Shadow Dash | Unique + Ultimate | rush, dmg 40, range 16, n 1, lock 20t | 1800 | 8 |
| Ambush Strike | Unique + Ultimate | rush, dmg 70, range 14, n 1 | 2600 | 12 |
| Claw Flurry (crouch) | Ultimate | barrage, dmg 8, range 8, n 10, lock 40t | 3200 | 14 |
| Veil of Night | Ultimate | veil, range 10, n 1 | 2200 | 20 |
| Eclipse of a Thousand Claws | mastery only | barrage, dmg 12, range 12, n 16, lock 60t | 20000 / 13333 mastered | 110 / 60 |

### Elderwood Sovereign, Heart of the Ancient Grove (`bw_elderwood_guardian`) and Will of the Elderwood Guardian (`bw_elderwood_guardian_unique`)

Will: `elderwood_guardian_will`. Elderwood Guardian: an ancient warden of the grove, rare tier. Power scale 0.64 (Unique 0.32). Finisher look: `G:TREE_ROOTS`.

| Passive (Ultimate: all; Unique: the first) | Effect |
|---|---|
| Bark Hide | armor +7 and knockback resistance |
| Heartwood Mend | heals 2.0% max health a second, cleanses every 5 s |
| Creeping Roots | slows up to 8 foes within 8 blocks |

| Active | Where | Move | Cost (Magicule) | Cooldown (s) |
|---|---|---|---|---|
| Vine Lash | Unique + Ultimate | beam, dmg 45, range 20, radius 1.0, n 4, lock 30t | 1800 | 8 |
| Bark Ward | Unique + Ultimate | guard, radius 8.0, n 10, lock 200t | 2400 | 16 |
| Restraining Roots (crouch) | Ultimate | seal, range 26, n 1, lock 100t | 3400 | 22 |
| Verdant Mend | Ultimate | capsule, radius 14.0, n 24 | 3000 | 24 |
| Heart of the Elder Grove | mastery only | blast, dmg 90, radius 14.0, n 24, lock 120t | 22000 / 14666 mastered | 120 / 66 |

### Lion Sovereign, King of the Golden Savanna (`bw_lion`) and Will of the Mana Lion (`bw_lion_unique`)

Will: `lion_will`. Lion: a pride king, rare tier. Power scale 0.68 (Unique 0.34). Finisher look: `L:FIRE_LION`.

| Passive (Ultimate: all; Unique: the first) | Effect |
|---|---|
| Mane of Might | attack damage +2.0 |
| Pride Bond | speed for up to 8 allies within 16 blocks |
| Lion's Courage | below 35% health: strength and resistance |

| Active | Where | Move | Cost (Magicule) | Cooldown (s) |
|---|---|---|---|---|
| Roar of the Pride | Unique + Ultimate | blast, dmg 45, radius 10.0, n 24, lock 40t | 2200 | 12 |
| Mane Surge | Unique + Ultimate | guard, radius 8.0, n 8, lock 200t | 2600 | 18 |
| Royal Pounce | Ultimate | rush, dmg 65, range 18, n 1, lock 20t | 2800 | 10 |
| Rend and Tear (crouch) | Ultimate | barrage, dmg 9, range 8, n 8, lock 30t | 3200 | 14 |
| Sovereign's Roar | mastery only | stop, dmg 100, range 24, radius 6.0, n 24, lock 100t | 24000 / 16000 mastered | 120 / 66 |

### Wolf Sovereign, Alpha of the Ridge (`bw_ridge_wolf`) and Will of the Ridge Wolf (`bw_ridge_wolf_unique`)

Will: `ridge_wolf_will`. Ridge Wolf: a pack hunter, common tier. Power scale 0.5 (Unique 0.25). Finisher look: `L:SLASH_WAVE`.

| Passive (Ultimate: all; Unique: the first) | Effect |
|---|---|
| Pack Runner | move speed +22% (attack speed half of that) |
| Keen Nose | glowing on up to 16 foes within 24 blocks |
| Pack Bond | speed for up to 8 allies within 20 blocks |

| Active | Where | Move | Cost (Magicule) | Cooldown (s) |
|---|---|---|---|---|
| Fang Bite | Unique + Ultimate | rush, dmg 35, range 14, n 2 | 1500 | 8 |
| Ridge Howl | Unique + Ultimate | blast, dmg 30, radius 9.0, n 24, lock 30t | 2000 | 14 |
| Pack Hunt (crouch) | Ultimate | barrage, dmg 7, range 8, n 12, lock 30t | 2600 | 14 |
| Pin Down | Ultimate | seal, range 20, n 1, lock 80t | 2400 | 16 |
| Hunt of a Thousand Fangs | mastery only | barrage, dmg 11, range 10, n 20, lock 60t | 15000 / 10000 mastered | 100 / 55 |

### Boar Sovereign, Unstoppable Tusk of the Highlands (`bw_crested_boar`) and Will of the Crested Boar (`bw_crested_boar_unique`)

Will: `crested_boar_will`. Crested Boar: a charging bruiser, common tier. Power scale 0.52 (Unique 0.26). Finisher look: `L:EARTH_RAMPAGE`.

| Passive (Ultimate: all; Unique: the first) | Effect |
|---|---|
| Thick Hide | armor +5 and knockback resistance |
| Unstoppable Charge | attack damage +1.5 |
| Boar's Fury | below 35% health: strength and resistance |

| Active | Where | Move | Cost (Magicule) | Cooldown (s) |
|---|---|---|---|---|
| Gore | Unique + Ultimate | rush, dmg 55, range 16, n 1, lock 20t | 1800 | 9 |
| Tusk Sweep | Unique + Ultimate | blast, dmg 40, radius 6.0, n 16, lock 30t | 2200 | 12 |
| Bulwark | Ultimate | guard, radius 6.0, n 8, lock 160t | 2400 | 18 |
| Trample (crouch) | Ultimate | stop, dmg 45, range 22, radius 3.0, n 16, lock 40t | 2800 | 14 |
| Crested Avalanche | mastery only | rush, dmg 90, range 24, n 3, lock 60t | 16000 / 10666 mastered | 100 / 55 |

### Skimmer Sovereign, Rider of the Endless Gale (`bw_wind_skimmer`) and Will of the Wind Skimmer (`bw_wind_skimmer_unique`)

Will: `wind_skimmer_will`. Wind Skimmer: a fast flier, common tier. Power scale 0.54 (Unique 0.27). Finisher look: `L:WIND_EMPEROR`.

| Passive (Ultimate: all; Unique: the first) | Effect |
|---|---|
| Gale Stride | move speed +30% (attack speed half of that) |
| Featherfall | slow fall and jump boost |
| Wind Veil | dodges 22% of melee hits (80% of projectiles) |

| Active | Where | Move | Cost (Magicule) | Cooldown (s) |
|---|---|---|---|---|
| Gale Cut | Unique + Ultimate | beam, dmg 40, range 36, radius 1.2, n 6 | 1700 | 7 |
| Skimming Dash | Unique + Ultimate | rush, dmg 35, range 22, n 1, lock 20t | 1800 | 9 |
| Cyclone Ring | Ultimate | blast, dmg 35, radius 8.0, n 16, lock 30t | 2400 | 13 |
| Wind Wall (crouch) | Ultimate | stop, dmg 30, range 20, radius 4.0, n 16, lock 20t | 2400 | 14 |
| Skimmer's Tempest | mastery only | beam, dmg 80, range 60, radius 3.0, n 24, lock 40t | 18000 / 12000 mastered | 100 / 55 |

### Serpent Sovereign, Coil of the Burning Sky (`bw_flame_serpent`) and Will of the Flame Serpent (`bw_flame_serpent_unique`)

Will: `flame_serpent_will`. Flame Serpent: a molten coil, rare tier. Power scale 0.66 (Unique 0.33). Finisher look: `L:FIRE_SPIRAL`.

| Passive (Ultimate: all; Unique: the first) | Effect |
|---|---|
| Ember Scales | sets up to 8 foes within 5 blocks alight, fire resistance |
| Serpent's Heat | armor +3 and knockback resistance |
| Coil Regeneration | heals 1.2% max health a second, cleanses every 5 s |

| Active | Where | Move | Cost (Magicule) | Cooldown (s) |
|---|---|---|---|---|
| Fire Fang | Unique + Ultimate | rush, dmg 50, range 14, n 1 | 1800 | 8 |
| Ember Spit | Unique + Ultimate | beam, dmg 55, range 40, radius 1.2, n 6 | 2000 | 9 |
| Scorching Coil | Ultimate | blast, dmg 50, radius 8.0, n 20, lock 40t | 2600 | 13 |
| Heat Haze (crouch) | Ultimate | veil, range 10, n 1 | 2200 | 18 |
| Serpent's Cremation | mastery only | beam, dmg 115, range 56, radius 3.0, n 24, lock 60t | 22000 / 14666 mastered | 120 / 66 |

### Chimera Sovereign, Three Heads of the Thunder (`bw_thunder_fang_chimera`) and Will of the Thunder Fang Chimera (`bw_thunder_fang_chimera_unique`)

Will: `thunder_fang_chimera_will`. Thunder Fang Chimera: a storm hybrid, epic tier. Power scale 0.72 (Unique 0.36). Finisher look: `G:LIGHTNING_GOD`.

| Passive (Ultimate: all; Unique: the first) | Effect |
|---|---|
| Thunder Pace | move speed +22% (attack speed half of that) |
| Chimera's Hunger | attack damage +2.0 |
| Storm Hide | armor +4 and knockback resistance |

| Active | Where | Move | Cost (Magicule) | Cooldown (s) |
|---|---|---|---|---|
| Thunder Fang | Unique + Ultimate | rush, dmg 60, range 18, n 1, lock 20t | 2000 | 8 |
| Chain Bolt | Unique + Ultimate | beam, dmg 60, range 46, radius 1.4, n 8, lock 30t | 2200 | 9 |
| Thunder Roar | Ultimate | blast, dmg 55, radius 9.0, n 24, lock 40t | 2800 | 14 |
| Triple-Headed Assault (crouch) | Ultimate | barrage, dmg 10, range 8, n 9, lock 30t | 3400 | 14 |
| Skyfall of the Chimera | mastery only | blast, dmg 110, range 40, radius 14.0, n 24, lock 100t | 24000 / 16000 mastered | 120 / 66 |

### Fenrir Sovereign, Soul-Light of the Giant Wolf (`bw_fenrir`) and Fenrir's Loyal Soul (`bw_fenrir_unique`)

Will: `fenrir_will`. Fenrir (God of War): the loyal wolf whose soul became a giant, legendary tier. Power scale 0.95 (Unique 0.475). Finisher look: `G:PAINT_BEAST`.

| Passive (Ultimate: all; Unique: the first) | Effect |
|---|---|
| Soul-Light | heals 2.0% max health a second, cleanses every 5 s |
| Frostbound Coat | slows up to 8 foes within 9 blocks |
| Loyal Guardian | speed for up to 8 allies within 18 blocks |

| Active | Where | Move | Cost (Magicule) | Cooldown (s) |
|---|---|---|---|---|
| Call of the Spirit Wolf | Unique + Ultimate | blast, dmg 55, radius 10.0, n 24, lock 40t | 2400 | 12 |
| Soul-Frost Breath | Unique + Ultimate | beam, dmg 80, range 48, radius 2.0, n 10, lock 50t | 3000 | 12 |
| Soul Seal | Ultimate | seal, range 28, n 1, lock 100t | 3600 | 22 |
| Garm's Pounce (crouch) | Ultimate | rush, dmg 60, range 20, n 2, lock 20t | 2800 | 10 |
| Soul of the Wolf, Unleashed | mastery only | blast, dmg 125, range 14, radius 12.0, n 24, lock 80t | 26000 / 17333 mastered | 130 / 71 |

### Jormungandr Sovereign, The Serpent Who Circles the World (`bw_jormungandr`) and Will of the World Serpent (`bw_jormungandr_unique`)

Will: `jormungandr_will`. Jormungandr: the world serpent, legendary tier. Power scale 0.96 (Unique 0.48). Finisher look: `L:WATER_DRAGON`.

| Passive (Ultimate: all; Unique: the first) | Effect |
|---|---|
| World-Serpent Scales | armor +7 and knockback resistance |
| Encircling Venom | poisons up to 8 foes within 6 blocks |
| Ocean Blood | water breathing and dolphin's grace |

| Active | Where | Move | Cost (Magicule) | Cooldown (s) |
|---|---|---|---|---|
| Venom Fang | Unique + Ultimate | rush, dmg 55, range 16, n 1, lock 20t | 2200 | 9 |
| Tidal Coil | Unique + Ultimate | blast, dmg 55, radius 10.0, n 24, lock 40t | 2800 | 13 |
| Constrict | Ultimate | seal, range 24, n 1, lock 120t | 3600 | 20 |
| Tail Lash (crouch) | Ultimate | stop, dmg 50, range 24, radius 4.0, n 20, lock 30t | 2600 | 12 |
| World-Encircling Coil | mastery only | blast, dmg 120, radius 18.0, n 24, lock 140t | 30000 / 20000 mastered | 140 / 77 |

### Cerberus Sovereign, Three Heads at the Gate of the Dead (`bw_cerberus`) and Will of the Hellhound (`bw_cerberus_unique`)

Will: `cerberus_will`. Cerberus: the three-headed hound of Hades, mythic tier. Power scale 0.82 (Unique 0.41). Finisher look: `F:SPACE_PORTAL`.

| Passive (Ultimate: all; Unique: the first) | Effect |
|---|---|
| Gatekeeper's Hide | armor +5 and knockback resistance |
| Three Heads' Vigil | glowing on up to 16 foes within 26 blocks |
| Hellhound Fury | below 35% health: strength and resistance |

| Active | Where | Move | Cost (Magicule) | Cooldown (s) |
|---|---|---|---|---|
| Hellfire Bite | Unique + Ultimate | rush, dmg 55, range 16, n 2 | 2200 | 9 |
| Triple Bark | Unique + Ultimate | blast, dmg 45, radius 9.0, n 24, lock 40t | 2400 | 12 |
| Hellfire Breath | Ultimate | beam, dmg 70, range 36, radius 2.5, n 10 | 3000 | 12 |
| Guard of the Gate (crouch) | Ultimate | guard, radius 7.0, n 8, lock 200t | 2600 | 18 |
| Triple Gate of Hades | mastery only | barrage, dmg 15, range 12, n 18, lock 80t | 24000 / 16000 mastered | 120 / 66 |

### Minotaur Sovereign, Master of the Labyrinth (`bw_minotaur`) and Will of the Labyrinth Bull (`bw_minotaur_unique`)

Will: `minotaur_will`. Minotaur: the bull of the labyrinth, epic tier. Power scale 0.72 (Unique 0.36). Finisher look: `G:EARTH_CLAWS`.

| Passive (Ultimate: all; Unique: the first) | Effect |
|---|---|
| Labyrinth Hide | armor +6 and knockback resistance |
| Bull's Rage | below 35% health: strength and resistance |
| Maze Sense | glowing on up to 16 foes within 24 blocks |

| Active | Where | Move | Cost (Magicule) | Cooldown (s) |
|---|---|---|---|---|
| Horn Charge | Unique + Ultimate | rush, dmg 70, range 20, n 1, lock 20t | 2400 | 9 |
| Labrys Cleave | Unique + Ultimate | beam, dmg 65, range 8, radius 3.0, n 6 | 2400 | 10 |
| Bellowing Challenge (crouch) | Ultimate | blast, dmg 30, radius 10.0, n 24, lock 80t | 2000 | 14 |
| Maze Wall | Ultimate | guard, radius 6.0, n 8, lock 180t | 2400 | 16 |
| Labyrinth's Last Charge | mastery only | rush, dmg 120, range 26, n 3, lock 60t | 20000 / 13333 mastered | 110 / 60 |

### Hydra Sovereign, The Head That Grows Back (`bw_hydra`) and Will of the Many-Headed Serpent (`bw_hydra_unique`)

Will: `hydra_will`. Hydra: the many-headed marsh serpent, mythic tier. Power scale 0.84 (Unique 0.42). Finisher look: `L:POISON_BREATH`.

| Passive (Ultimate: all; Unique: the first) | Effect |
|---|---|
| Regrowing Flesh | heals 2.5% max health a second, cleanses every 5 s |
| Hydra Venom | poisons up to 8 foes within 7 blocks |
| Many Heads' Vigil | glowing on up to 16 foes within 24 blocks |

| Active | Where | Move | Cost (Magicule) | Cooldown (s) |
|---|---|---|---|---|
| Venom Bite | Unique + Ultimate | rush, dmg 45, range 16, n 2 | 2000 | 9 |
| Acid Spray | Unique + Ultimate | beam, dmg 50, range 30, radius 3.0, n 12 | 2400 | 10 |
| Multi-Head Strike (crouch) | Ultimate | barrage, dmg 8, range 8, n 9, lock 30t | 3200 | 14 |
| Heads Together | Ultimate | guard, radius 6.0, n 8, lock 180t | 2600 | 18 |
| Regrowing Heads | mastery only | barrage, dmg 14, range 10, n 22, lock 80t | 22000 / 14666 mastered | 110 / 60 |

### Kraken Sovereign, Terror of the Deep Sea (`bw_kraken`) and Will of the Deep Terror (`bw_kraken_unique`)

Will: `kraken_will`. Kraken: the tentacled terror of the deep, mythic tier. Power scale 0.88 (Unique 0.44). Finisher look: `G:WATER_NEST`.

| Passive (Ultimate: all; Unique: the first) | Effect |
|---|---|
| Abyssal Hide | armor +4 and knockback resistance |
| Tide Caller | water breathing and dolphin's grace |
| Ink Cloak | invisible while crouching and unhurt |

| Active | Where | Move | Cost (Magicule) | Cooldown (s) |
|---|---|---|---|---|
| Tentacle Lash | Unique + Ultimate | beam, dmg 60, range 14, radius 2.0, n 6, lock 30t | 2200 | 10 |
| Ink Burst | Unique + Ultimate | veil, range 10, n 1 | 2200 | 18 |
| Crushing Grip | Ultimate | seal, range 24, n 1, lock 120t | 3600 | 20 |
| Maelstrom Pull (crouch) | Ultimate | blast, dmg 50, radius 10.0, n 24, lock 40t | 2800 | 14 |
| Fleet-Drowning Maelstrom | mastery only | blast, dmg 115, radius 18.0, n 24, lock 120t | 28000 / 18666 mastered | 130 / 71 |

### Hafgufa Sovereign, The Island That Swallows Fleets (`bw_hafgufa`) and Will of the Island-Fish (`bw_hafgufa_unique`)

Will: `hafgufa_will`. Hafgufa: the island-sized sea beast, mythic tier. Power scale 0.8 (Unique 0.4). Finisher look: `F:FOOD_MAW`.

| Passive (Ultimate: all; Unique: the first) | Effect |
|---|---|
| Island Hide | armor +8 and knockback resistance |
| Deep Tide | water breathing and dolphin's grace |
| Bait Glow | glowing on up to 16 foes within 30 blocks |

| Active | Where | Move | Cost (Magicule) | Cooldown (s) |
|---|---|---|---|---|
| Gaping Maw | Unique + Ultimate | blast, dmg 55, radius 9.0, n 20, lock 40t | 2600 | 13 |
| Sweet Breath | Unique + Ultimate | seal, range 26, n 1, lock 100t | 3000 | 20 |
| Tidal Spout | Ultimate | beam, dmg 60, range 40, radius 2.0, n 10 | 2400 | 10 |
| Island Back (crouch) | Ultimate | guard, radius 8.0, n 10, lock 200t | 2600 | 18 |
| The Island Swallows the Fleet | mastery only | blast, dmg 110, range 6, radius 16.0, n 24, lock 100t | 26000 / 17333 mastered | 130 / 71 |

### Typhon Sovereign, Father of All Monsters (`bw_typhon`) and Will of the Storm Giant (`bw_typhon_unique`)

Will: `typhon_will`. Typhon: the father of monsters, legendary tier. Power scale 1.0 (Unique 0.5). Finisher look: `G:LIGHTNING_GOD`.

| Passive (Ultimate: all; Unique: the first) | Effect |
|---|---|
| Hundred Throats | attack damage +3.0 |
| Storm Hide | armor +6 and knockback resistance |
| Serpent Legs | slow fall and jump boost |
| Monstrous Vitality | heals 1.5% max health a second, cleanses every 5 s |

| Active | Where | Move | Cost (Magicule) | Cooldown (s) |
|---|---|---|---|---|
| Storm Bolt | Unique + Ultimate | beam, dmg 90, range 52, radius 2.0, n 12, lock 40t | 3200 | 11 |
| Serpent Coil Lash | Unique + Ultimate | blast, dmg 60, radius 10.0, n 24, lock 50t | 3000 | 12 |
| Venom Torrent (crouch) | Ultimate | stop, dmg 60, range 30, radius 5.0, n 24, lock 40t | 3000 | 14 |
| Fire Breath | Ultimate | beam, dmg 85, range 44, radius 3.0, n 12 | 3400 | 13 |
| Hundred-Headed Tempest | mastery only | stop, dmg 125, range 40, radius 7.0, n 24, lock 120t | 30000 / 20000 mastered | 150 / 82 |

### Strait Sovereign, Scylla Above, Charybdis Below (`bw_scylla_charybdis`) and Will of the Devouring Strait (`bw_scylla_charybdis_unique`)

Will: `scylla_charybdis_will`. Scylla and Charybdis: the six-headed rock and the whirlpool, mythic tier. Power scale 0.8 (Unique 0.4). Finisher look: `G:WATER_NEST`.

| Passive (Ultimate: all; Unique: the first) | Effect |
|---|---|
| Whirlpool Hide | water breathing and dolphin's grace |
| Six Heads' Vigil | glowing on up to 16 foes within 24 blocks |
| Narrow Strait | armor +4 and knockback resistance |

| Active | Where | Move | Cost (Magicule) | Cooldown (s) |
|---|---|---|---|---|
| Scylla's Snatch | Unique + Ultimate | rush, dmg 60, range 16, n 2, lock 20t | 2400 | 9 |
| Charybdis Whirl | Unique + Ultimate | blast, dmg 50, radius 9.0, n 24, lock 40t | 2800 | 13 |
| Six-Headed Bite (crouch) | Ultimate | barrage, dmg 9, range 8, n 6, lock 30t | 3000 | 14 |
| Strait Ward | Ultimate | guard, radius 7.0, n 8, lock 180t | 2600 | 18 |
| Between Scylla and Charybdis | mastery only | blast, dmg 115, radius 16.0, n 24, lock 120t | 26000 / 17333 mastered | 130 / 71 |

### Sphinx Sovereign, Keeper of the Last Riddle (`bw_sphinx`) and Will of the Riddler (`bw_sphinx_unique`)

Will: `sphinx_will`. Sphinx: the riddling guardian, mythic tier. Power scale 0.78 (Unique 0.39). Finisher look: `G:GAME_BOARD`.

| Passive (Ultimate: all; Unique: the first) | Effect |
|---|---|
| Riddler's Gaze | glowing on up to 16 foes within 30 blocks |
| Stone Poise | armor +5 and knockback resistance |
| Mirage Veil | dodges 25% of melee hits (80% of projectiles) |

| Active | Where | Move | Cost (Magicule) | Cooldown (s) |
|---|---|---|---|---|
| Riddle Gaze | Unique + Ultimate | seal, range 28, n 1, lock 100t | 3000 | 18 |
| Stone Paw | Unique + Ultimate | rush, dmg 55, range 16, n 1, lock 20t | 2400 | 9 |
| Sandstorm Wing (crouch) | Ultimate | stop, dmg 40, range 22, radius 5.0, n 20, lock 30t | 2400 | 12 |
| Enigma Mark | Ultimate | scan, radius 24.0, n 24 | 2000 | 14 |
| Riddle of Judgement | mastery only | psychic, dmg 80, range 40, n 3, lock 140t | 24000 / 16000 mastered | 130 / 71 |

### Pegasus Sovereign, Star-Mane of the Heavens (`bw_pegasus`) and Will of the Winged Steed (`bw_pegasus_unique`)

Will: `pegasus_will`. Pegasus: the winged steed of the heavens, epic tier. Power scale 0.7 (Unique 0.35). Finisher look: `L:LIGHT_BLADE`.

| Passive (Ultimate: all; Unique: the first) | Effect |
|---|---|
| Winged Stride | move speed +25% (attack speed half of that) |
| Sky Dancer | slow fall and jump boost |
| Holy Mane | heals 1.5% max health a second, cleanses every 5 s |

| Active | Where | Move | Cost (Magicule) | Cooldown (s) |
|---|---|---|---|---|
| Sky Charge | Unique + Ultimate | rush, dmg 55, range 24, n 1, lock 20t | 2200 | 8 |
| Wing Gale | Unique + Ultimate | stop, dmg 35, range 24, radius 4.0, n 16, lock 20t | 2400 | 12 |
| Radiant Mane | Ultimate | capsule, radius 12.0, n 24 | 3000 | 22 |
| Starlight Hooves (crouch) | Ultimate | barrage, dmg 9, range 8, n 8, lock 20t | 3000 | 12 |
| Meteor Charge | mastery only | rush, dmg 125, range 36, n 4, lock 40t | 20000 / 13333 mastered | 110 / 60 |

### Cu Sith Sovereign, Green Hound of the Fairy Hills (`bw_cu_sith`) and Will of the Faerie Hound (`bw_cu_sith_unique`)

Will: `cu_sith_will`. Cu Sith: the silent green faerie hound, rare tier. Power scale 0.62 (Unique 0.31). Finisher look: `L:WIND_GALE`.

| Passive (Ultimate: all; Unique: the first) | Effect |
|---|---|
| Silent Paws | invisible while crouching and unhurt |
| Hound's Pace | move speed +25% (attack speed half of that) |
| Faerie Senses | glowing on up to 16 foes within 26 blocks |

| Active | Where | Move | Cost (Magicule) | Cooldown (s) |
|---|---|---|---|---|
| Spectral Snap | Unique + Ultimate | rush, dmg 40, range 16, n 1 | 1800 | 8 |
| Three Barks | Unique + Ultimate | blast, dmg 35, radius 9.0, n 24, lock 60t | 2400 | 14 |
| Hunter's Mark | Ultimate | scan, radius 24.0, n 24 | 2000 | 14 |
| Faerie Mist (crouch) | Ultimate | veil, range 10, n 1 | 2400 | 18 |
| Hunt of the Green Hound | mastery only | barrage, dmg 10, range 14, n 18, lock 60t | 17000 / 11333 mastered | 100 / 55 |

### Nidhoggr Sovereign, Gnawer at the Root of the World (`bw_nidhoggr`) and Will of the Root-Gnawer (`bw_nidhoggr_unique`)

Will: `nidhoggr_will`. Nidhoggr: the dragon gnawing the world tree, legendary tier. Power scale 0.94 (Unique 0.47). Finisher look: `G:KOTO_SLUDGE`.

| Passive (Ultimate: all; Unique: the first) | Effect |
|---|---|
| Root Gnawer | withers up to 8 foes within 6 blocks |
| Corpse Hide | armor +6 and knockback resistance |
| Deathless Hunger | below 35% health: strength and resistance |

| Active | Where | Move | Cost (Magicule) | Cooldown (s) |
|---|---|---|---|---|
| Root Bite | Unique + Ultimate | rush, dmg 65, range 16, n 2 | 2400 | 9 |
| Corpse Breath | Unique + Ultimate | beam, dmg 70, range 40, radius 2.5, n 10 | 3000 | 11 |
| Hall of the Dead | Ultimate | blast, dmg 55, radius 9.0, n 20, lock 40t | 2800 | 13 |
| Dread Hide (crouch) | Ultimate | guard, radius 7.0, n 8, lock 200t | 2800 | 18 |
| Ragnarok's Gnawing | mastery only | beam, dmg 135, range 60, radius 4.0, n 24, lock 80t | 28000 / 18666 mastered | 130 / 71 |

### Orochi Sovereign, Eight Heads, Eight Valleys (`bw_yamata_no_orochi`) and Will of the Eight-Headed Serpent (`bw_yamata_no_orochi_unique`)

Will: `yamata_no_orochi_will`. Yamata no Orochi: the eight-headed serpent, legendary tier. Power scale 0.98 (Unique 0.49). Finisher look: `L:WATER_DRAGON`.

| Passive (Ultimate: all; Unique: the first) | Effect |
|---|---|
| Eight Heads' Vigil | glowing on up to 16 foes within 28 blocks |
| Eight-Tail Scales | armor +6 and knockback resistance |
| Orochi Regeneration | heals 2.0% max health a second, cleanses every 5 s |

| Active | Where | Move | Cost (Magicule) | Cooldown (s) |
|---|---|---|---|---|
| Head Strike | Unique + Ultimate | rush, dmg 65, range 16, n 2, lock 20t | 2400 | 8 |
| Poison Sake Mist | Unique + Ultimate | blast, dmg 50, radius 10.0, n 24, lock 40t | 2800 | 13 |
| Eight-Head Bite (crouch) | Ultimate | barrage, dmg 9, range 8, n 8, lock 30t | 3200 | 14 |
| Valley Flood | Ultimate | stop, dmg 55, range 28, radius 5.0, n 24, lock 40t | 3000 | 14 |
| Eight-Head Calamity | mastery only | barrage, dmg 16, range 14, n 24, lock 100t | 30000 / 20000 mastered | 150 / 82 |

### Kitsune Sovereign, Nine Tails of the Foxfire (`bw_kitsune`) and Will of the Fox Spirit (`bw_kitsune_unique`)

Will: `kitsune_will`. Kitsune: the nine-tailed fox spirit, epic tier. Power scale 0.76 (Unique 0.38). Finisher look: `G:FIRE_WILD`.

| Passive (Ultimate: all; Unique: the first) | Effect |
|---|---|
| Illusionist | dodges 30% of melee hits (80% of projectiles) |
| Foxfire Mantle | sets up to 8 foes within 4 blocks alight, fire resistance |
| Nine Tails' Grace | move speed +20% (attack speed half of that) |

| Active | Where | Move | Cost (Magicule) | Cooldown (s) |
|---|---|---|---|---|
| Foxfire | Unique + Ultimate | beam, dmg 55, range 40, radius 1.4, n 6 | 2200 | 9 |
| Fox Glamour | Unique + Ultimate | veil, range 10, n 1 | 2400 | 18 |
| Illusory Bind | Ultimate | seal, range 26, n 1, lock 100t | 3200 | 20 |
| Tail Whip (crouch) | Ultimate | stop, dmg 40, range 20, radius 4.0, n 16, lock 20t | 2400 | 12 |
| Nine-Tailed Foxfire Rite | mastery only | blast, dmg 105, radius 16.0, n 24, lock 100t | 24000 / 16000 mastered | 120 / 66 |

### Nue Sovereign, Omen of the Black Cloud (`bw_nue`) and Will of the Night-Cry Chimera (`bw_nue_unique`)

Will: `nue_will`. Nue: the ominous chimera of the night, epic tier. Power scale 0.74 (Unique 0.37). Finisher look: `G:LIGHTNING_GOD`.

| Passive (Ultimate: all; Unique: the first) | Effect |
|---|---|
| Black Cloud Veil | invisible while crouching and unhurt |
| Tiger Strength | attack damage +2.0 |
| Serpent Tail | poisons up to 8 foes within 5 blocks |

| Active | Where | Move | Cost (Magicule) | Cooldown (s) |
|---|---|---|---|---|
| Thunder Cry | Unique + Ultimate | blast, dmg 50, radius 9.0, n 24, lock 40t | 2400 | 13 |
| Shadow Flit | Unique + Ultimate | rush, dmg 50, range 22, n 1, lock 20t | 2200 | 9 |
| Serpent Tail Whip | Ultimate | beam, dmg 50, range 12, radius 2.0, n 6 | 2000 | 9 |
| Eerie Night Cry (crouch) | Ultimate | seal, range 24, n 1, lock 100t | 3000 | 20 |
| Black Cloud Calamity | mastery only | beam, dmg 110, range 50, radius 4.0, n 24, lock 80t | 22000 / 14666 mastered | 120 / 66 |

### Ammit Sovereign, Devourer of the Unworthy Heart (`bw_ammit`) and Will of the Heart-Eater (`bw_ammit_unique`)

Will: `ammit_will`. Ammit: the devourer of the dead, mythic tier. Power scale 0.76 (Unique 0.38). Finisher look: `F:FOOD_MAW`.

| Passive (Ultimate: all; Unique: the first) | Effect |
|---|---|
| Heart Weigher | glowing on up to 16 foes within 28 blocks |
| Devourer's Hide | armor +5 and knockback resistance |
| Crocodile Jaws | attack damage +2.0 |

| Active | Where | Move | Cost (Magicule) | Cooldown (s) |
|---|---|---|---|---|
| Devouring Bite | Unique + Ultimate | rush, dmg 65, range 16, n 1 | 2400 | 9 |
| Judgement Gaze | Unique + Ultimate | seal, range 26, n 1, lock 100t | 3200 | 20 |
| Hippo Stomp | Ultimate | blast, dmg 50, radius 8.0, n 20, lock 40t | 2600 | 13 |
| Scales of Ma'at (crouch) | Ultimate | scan, radius 22.0, n 24 | 2200 | 16 |
| Devourer of the Unworthy Heart | mastery only | disc, dmg 145, range 40, radius 1.4, n 1 | 26000 / 17333 mastered | 130 / 71 |

### Apophis Sovereign, Serpent of the Unending Night (`bw_apophis`) and Will of the Chaos Serpent (`bw_apophis_unique`)

Will: `apophis_will`. Apophis: the serpent of chaos who swallows the sun, legendary tier. Power scale 0.97 (Unique 0.485). Finisher look: `G:NIHILITY_ZONE`.

| Passive (Ultimate: all; Unique: the first) | Effect |
|---|---|
| Chaos Blight | withers up to 8 foes within 7 blocks |
| Darkness Cloak | invisible while crouching and unhurt |
| Endless Hunger | heals 1.5% max health a second, cleanses every 5 s |

| Active | Where | Move | Cost (Magicule) | Cooldown (s) |
|---|---|---|---|---|
| Hypnotic Gaze | Unique + Ultimate | seal, range 28, n 1, lock 100t | 3200 | 18 |
| Coil of Chaos | Unique + Ultimate | blast, dmg 60, radius 10.0, n 24, lock 40t | 3000 | 13 |
| Venom Spit | Ultimate | beam, dmg 75, range 40, radius 2.0, n 10 | 3000 | 11 |
| Veil of Darkness (crouch) | Ultimate | veil, range 10, n 1 | 2800 | 20 |
| Night Without Dawn | mastery only | blast, dmg 130, radius 20.0, n 24, lock 160t | 30000 / 20000 mastered | 150 / 82 |

