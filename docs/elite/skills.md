# Elite skills: 21 skills, 7 Gaia parts and the evolution trigger engine (1.83.0 job, area `skills`)

Why: the owner's "EliteTensuraAddon" inventory names 21 skills, the seven parts of Gaia and twelve skill-evolution requirement triggers. This area builds all of them as real ManasCore / Tensura skills in
`com.newuniverse.nusmp.elite.skills`, next to the 38 rebuilt skills and the Grimoire, and replaces nothing. **The original addon's source was not available**: only the names were. Every mechanic, number and
rule below is this mod's own design, made from what the names mean in Tensura and Dragon Ball. The numbers were never played; they sit in pure rule classes (JUnit-tested relations) and the config, so the first
thing to turn is `nusmp-elite-skills-server.toml`.

## 1. Comparison with what the mod already has (CLAUDE.md rule 5)

I compared every name with the 38 rebuilt skills (`skill/**`, `docs/skills/rebuild.md`), `NUSkills`, the Grimoire books and pages, the Tensura API dump (`ability.magic.aspectual.*`: gravity has Burden, Float,
Lighten; lightning has Thunder, Thunder Lance, Thunder Orb, Thunder Rain; no Gravity Well, Static Chain or Kamehameha), the Tensura 2.0.1.3 skill id list the owner uploaded (`gravity_domination`, `gravity_field`,
`gravity_manipulation`, `summon_basilisk`, `scale_armor`, `dragon_skin`, ... but no `scavenger`, `hephaestus`, `beelzebuth`, `raphael`), the skill-line grid, the relics and the mobs.

| Inventory name | Verdict | Evidence and decision |
|---|---|---|
| `GravityWellMagic` | **NEW** | A Tensura *aspectual magic* does not exist for it. The Gravity grimoire has a page `gravity_well` and the mod has `VfxShape.GRAVITY_WELL`: a different, older spell in another system. Kept untouched; the shape is reused for the look. Id `gravity_well`. |
| `KamehamehaMagic` | **NEW** | Nothing like it (grep of `src`, `tools`, the Tensura dump). Id `kamehameha`. |
| `StaticChainMagic` | **NEW** | Only `boss/arc/LuciusChains` (a boss attack) matches the words; unrelated. Id `static_chain`. |
| `AncientSoulSkill` | **NEW** | Id `ancient_soul`. |
| `IronResolveSkill` | **NEW** | Unbreakable Will / Immortal Will refuse a death; this one stacks protection while wounded. Different. Id `iron_resolve`. |
| `MeditationSkill` | **NEW** | Only skill-line text mentions meditation. Id `meditation`. |
| `RunesmithsInsightSkill` | **NEW** | Id `runesmiths_insight`. |
| `ScavengerSkill` | **NEW** | The relic "Mantle of the Scavenger" (`dungeon/nexus/relic`) is an item with its own effect; kept. Id `scavenger`. |
| `KaiokenSkill` | **NEW** | Id `kaioken`. |
| `MechanicalEyeSkill` | **NEW** | Hawk Eye (toggle: night vision, glowing hostiles) and Six Eyes exist; this one analyses and marks, a different mechanic. Id `mechanical_eye`. |
| `MechanicalHandsSkill` | **NEW** | Id `mechanical_hands`. |
| `CrocodileSkinUnique` | **NEW** | Id `crocodile_skin`. |
| `HecateSkill` | **NEW** | Id `hecate`. |
| `VoidEdgeSkill` | **NEW** | Id `void_edge`. |
| `BasiliskSkinSkill` | **NEW** | The Basilisk mob, the Tensura `summon_basilisk` skill and `scale_armor` / `dragon_skin` exist; none is this skill. Id `basilisk_skin`. |
| `BeelzebuthSkill` | **NEW** | `boss/rules/BossSkillPolicy` only lists the *name* among Ultimate ids, and the skill-line grid has an ability "Maw of Beelzebuth" (`tools/lines`): neither is a ManasSkill. Both kept. Id `beelzebuth`. |
| `GaiaSkill` and its parts `GaiaBlockGuard`, `GaiaBloomLedger`, `GaiaCost`, `GaiaPillars`, `GaiaWarCheck`, `GaiaZone`, `VeinBloom` | **NEW** (all seven) | Id `gaia`. |
| `HephaestusSkill` | **ALREADY EXISTS (as the Unique), kept; the Ultimate is NEW** | `nusmp:hephaestus` is the DanMachi goddess of the forge (`skill/HephaestusSkill`, repairs the held item). It is untouched. The inventory lists Hephaestus as an *Ultimate*, so the Ultimate tier is added next to it as `hephaestus_ultimate` ("Hephaestus, Forgefather") and is the evolution target of the existing Unique (data file). Nothing duplicated, nothing replaced. |
| `OuroborosSkill` | **NEW** | Id `ouroboros`. |
| `RaphealSkill` | **NEW** | Spelled as in the inventory. Id `rapheal`. |
| `VoidSovereignSkill` | **NEW** | Id `void_sovereign`. |
| The 12 triggers (`AbilityMastered`, `ActiveTitleEvolution`, `AwakeningEvolution`, `EPGate`, `ForgeCraftedCount`, `ForgeQuality`, `HasTitle`, `HeldTitleCount`, `ItemConsume`, `KillEvolution`, `RecordUnlocked`, `ResetCount`) | **NEW** | The existing `SkillEvolution` (10 hard-coded Unique to Ultimate pairs, "mastered + EP") is **kept and untouched** and keeps running; the engine is an *additional*, data-driven path. |

**REPLACEMENT: none.** Nothing existing was removed, renamed, re-registered or re-numbered. Old skill ids, icons and saves are exactly as before.

## 2. What was built (files)

| Piece | File(s) | Job |
|---|---|---|
| Registry, the one wiring line | `elite/skills/EliteSkills.java`; `elite/EliteSystem.java` (one line in the skills section) | `DeferredRegister<ManasSkill>` with the 21 ids (pattern of `NUSkills`), registers the config, handlers, hook and self-tests. |
| Config | `EliteSkillsConfig.java` -> `nusmp-elite-skills-server.toml` | master switch and dials (section 6). |
| Base classes | `EliteSkill` (extends `NUSkill`: icon, translatable text, tier), `EliteAspectual` (extends Tensura `AspectualMagic`), `EliteMode` | one script for gate, pay, refund on failure, cooldown, cast circle, mastery. |
| Bridge | `skill/EliteBridge.java` (NEW file in an existing package, no existing class edited) | the rebuilt skills' helpers (who a skill may hurt, a hit through the balance law, Rukh-only effects, zones, cost multipliers) are package-private; this public facade hands them over in one line each instead of copying them. |
| Rules (pure, no Minecraft) | `rules/MagicRules`, `ExtraRules`, `IntrinsicRules`, `UniqueRules`, `UltimateRules`, `SnapshotRing`; `GaiaCost`, `GaiaZone`, `GaiaWarCheck`, `GaiaBloomLedger`, `VeinBloom`, `GaiaPillars` | every number and decision. |
| The 21 skills | `GravityWellMagic`, `KamehamehaMagic`, `StaticChainMagic`, `AncientSoulSkill`, `IronResolveSkill`, `MeditationSkill`, `RunesmithsInsightSkill`, `ScavengerSkill`, `KaiokenSkill`, `MechanicalEyeSkill`, `MechanicalHandsSkill`, `CrocodileSkinUnique`, `HecateSkill`, `VoidEdgeSkill`, `BasiliskSkinSkill`, `BeelzebuthSkill`, `GaiaSkill` (+ `GaiaWorld`, `GaiaBlockGuard`), `HephaestusSkill`, `OuroborosSkill`, `RaphealSkill`, `VoidSovereignSkill` | the mechanics (section 3). |
| Handlers | `EliteSkillEvents` (tick upkeep, kills, damage multipliers, runes, knock-back, loot, teleport, lifecycle), `EliteDefer` (one-tick queue), `EliteUtil`, `ResetLedger` | ManasCore only ticks passive skills now and then, so what must not depend on that is driven from NeoForge game-bus events. |
| Evolution engine | `elite/skills/evolution/*` (section 5) | triggers, table, engine, runtime. |
| Data | `data/nusmp/elite/skills/evolutions.json` | skill id -> target id -> list of `{trigger, params}`. |
| Icons | `tools/elite/gen_skill_icons.py` -> 21 PNGs in `assets/nusmp/textures/skill/` | 32x32, no two alike and none like an older icon (the script refuses otherwise). |
| Lang | `tools/elite/gen_skills_lang.py` -> `tools/elite/skills_lang.json` (172 keys) | read from the source; `en_us.json` is NOT edited. |
| Tests | `src/test/.../elite/skills/*Test.java`, `EliteSkillsSelfTests` | section 7. |

Effects: only Light / Dark Rukh (`RukhFx` through the bridge) and the mod's own `VfxShape`s (`GRAVITY_*`, `BORN_BEAM`, `LIGHTNING_SPEAR`, `FX_LIGHTNING_ARC`, `SOUL_VORTEX`, `EYE_FX1`, `EARTH_RISE`, `SPACE_*`,
`NIHILITY_ZONE`, ...). No vanilla particle and no Time Magic shape or file (a JUnit test greps for it). Damage goes through `BalanceLaw` (EP scaling, PvP cap) and every hard control (Gravity Well's pull,
Static Chain's tether, Death Roll's grip, Petrifying Gaze, the Crone's blind, Maw's drag, Gaia's launch) obeys the shared 12-second control lock (`BalanceLaw.beginControl`). Allies, pets, summons and the
caster are never hit (`SkillTargets`).

## 3. The skills

Costs are magicules before the pack-wide and elite multipliers; cooldowns in ticks. "Mastered" = the skill itself mastered. Grant in game: `/tensura edit <player> ability grant nusmp:<id>`.

### Aspectual magic (real Tensura `AspectualMagic`; cast on key RELEASE)
| Skill (id) | Type | Mechanic |
|---|---|---|
| Kamehameha (`kamehameha`) | MISC | HOLD to gather ki: one level per second (0.7 s mastered), five levels; RELEASE fires one piercing beam along your sight (18 + 8 x level blocks, 0.8 + 0.35 x level wide) striking every foe on it, stopping at the first wall. Damage 7 + 6.5 x level (x1.15 mastered), cost 350 + 300 x level (pay level 1 on release, the rest as the beam goes; if you cannot afford the level reached, fired at the highest you can), cooldown 100 + 50 x level, Slowness from level 2. Letting go before level 1 fizzles and costs nothing. |
| Gravity Well (`gravity_well`) | GRAVITY | Opens a well where you look (24 blocks): radius 5 (7), 6 s (8 s), pulses every half second: pull toward the centre (stronger nearer, bosses pulled 40%), 2-3 crush damage, Slowness; ends in a Collapse (4 pulses of damage + 6 at the centre). Mode 2 Collapse ends your own well early for a tenth of the price (600, cd 240). |
| Static Chain (`static_chain`) | LIGHTNING | Bolt on the foe you look at (20 blocks), jumps to the nearest foe within 7 blocks, 4 jumps (6), 15% weaker per link (12 damage); then tethers the victims for 3 s (4): anyone more than 5 blocks from the middle of the chain is yanked back. Cost 700, cd 160. |

### Extra
| Skill | Mechanic |
|---|---|
| Ancient Soul (`ancient_soul`) | Passive: kills leave fragments (boss 5, cap 100); 12% (20%) of any magic never lands. Press: Echo of Ages spends 20 fragments, heals 30% (45%) and cleanses. Cost 250, cd 200. |
| Iron Resolve (`iron_resolve`) | Passive: each hit taken below half health stacks Resolve (5, 7 mastered; 8 s), 5% less damage per stack (max 45%), no knock-back from 3 stacks. Press: Brace 3 s, half damage, no knock-back. Cost 300, cd 400. |
| Meditation (`meditation`) | Toggle: still and unhurt builds Focus (1 per 6 s, max 5); each Focus returns a growing share of max magicules per second (0.65% to 2%, x1.4 mastered) and from Focus 3 health. Moving or a hit resets it. Free to hold. |
| Runesmith's Insight (`runesmiths_insight`) | Modes: Read Runes (free: enchantments, durability, rune and time left) and Inscribe Fire / Frost / Thunder / Vitality on the damageable item in hand for 5 min (10): ignite / slow / +20% lightning / +5% lifesteal on melee hits. The rune lives in the item's own data. Cost 400, cd 100. |
| Scavenger (`scavenger`) | Passive: a kill duplicates one random drop stack with 20% (35%), at most one extra stack, never from players. Press: Sniff Out makes dropped items within 32 (48) blocks glow 20 s and reports the count and nearest distance. Cost 150, cd 300. |

### Intrinsic
| Skill | Mechanic |
|---|---|
| Kaioken (`kaioken`) | Toggle; the mode is the rung: x2, x3, x4, then x10 and x20 once mastered. Melee damage +30% / +60% / +100% / +200% / +400% and speed (capped at +100%); burns 1% / 2% / 3% / 6% / 10% of MAX health per second plus magicules; shuts off before it can kill (below 4 HP or 15% health) or when magicules run out; leaves Weakness and Slowness for longer from a higher rung. |
| Mechanical Eye (`mechanical_eye`) | Scan Pulse (120): foes within 24 (40) blocks glow 5 s, the nearest is read out (health, armour, damage). Weak Point (200): marks the foe you look at: your hits on it +20% for 10 s. |
| Mechanical Hands (`mechanical_hands`) | Extend Arms (250): +3 block reach, +1.5 entity reach, +15% swing speed for 60 s (120). Telekinesis (120): pulls dropped items and XP orbs within 12 (18) blocks. Mend (150): restores 15% (25%) of the held tool's durability. |

### Unique
| Skill | Mechanic |
|---|---|
| Crocodile Skin (`crocodile_skin`) | Passive: 20% (30%) of ordinary damage bounces off (not magic, not fall / void), +15 points in water, where melee also hits 25% harder. Press: Death Roll grips the foe you look at (6 blocks) and rolls it 3 times, 9 damage each, healing you a quarter of it. Cost 500, cd 400. |
| Hecate (`hecate`) | Three modes = three faces of the moon: Maiden's piercing Moonbolt (16 damage, 36 blocks, marks with a glow, quickens you), Mother's Blessing (heal 30% to everyone within 10, cleanse, regeneration), Crone's Hex (weaken and slow foes within 9, hunters forget you, blind if the control lock is free, you vanish 4 s). Each face is x1.4 at its own moon phase (Maiden waxing, Mother full, Crone new), x0.85 at the opposite, x0.7 by day. |
| Void Edge (`void_edge`) | Imbue Edge (450, 20 s / 30 s): every melee blow also deals a second armour-ignoring hit worth 15% + 2% per armour point (max 60%) of the blow, breaks a blocking player's shield and strips 30% armour for 6 s. Void Slash (350): a cutting wave 10 blocks long, 14 damage that ignores armour. |

### Ultimate
| Skill | Mechanic |
|---|---|
| Basilisk Skin (`basilisk_skin`) | Passive: 35% (42%) of ordinary damage turned aside, poison and wither cannot take hold, 18% (25%) of a melee blow thrown back as venom. Petrifying Gaze (900): foes in a 14-block cone crawl, weakened and fatigued 5 s (2 s for players), 10 damage. Molt (1500): cleanse and a shield of 40% (50%) of max health. |
| Beelzebuth (`beelzebuth`) | Passive: kills heal 10% (boss 30%), return 5% (15%) magicules and stack Hunger (max 10, boss 3, 1 min): +4% damage per stack to everything you hit. Devour (700): 24-damage bite, half returned as health. Maw (1400): drags foes within 11 blocks and bites them. Domain (2200): 6 s of 4%-of-max-health bites within 8 blocks, half heals you. |
| Gaia (`gaia`) | Raise Domain (14 blocks, 20 mastered; 2 min, 4): allies blessed, enemies of your nation burdened, strangers cannot break or blow up blocks; upkeep per second from `GaiaCost` (more on a war footing). Pillars: a line of 3 (5) earth pillars, 18 damage, knock-up, no blocks placed. Bloom: spends ledger points to grow a 3-6 (5-9) block vein of ore in natural stone, at most 8 a game day. Details in section 4. |
| Hephaestus, Forgefather (`hephaestus_ultimate`) | Reforge (3500): every damageable item you carry restored. Temper (1200): the weapon in hand +20% (+30%) damage for 10 min (20). Molten Anvil (1600): 30 damage and fire within 3.5 blocks where you look. |
| Ouroboros (`ouroboros`) | Remembers one moment a second for 20 s. Rewind (2000): back to where you were 10 s (15 s) ago with at least half health, cleansed. Cycle (3000, cd 10 min): every other cooldown reset, magicules to half (75%), recorded as a skill reset. Passive: a killing blow (not void, not /kill) is undone by an automatic Rewind once per 5 min. |
| Rapheal (`rapheal`) | Passive: 18% (25%, +15% while accelerated) of blows are foreseen and negated, at most one per second. Analyze (600): read out and mark a foe (+15% from you, 20 s). Thought Acceleration (1800): 10 s of speed and haste, a quarter of every other cooldown runs off each second. Dissect (1500): strips a foe's beneficial effects and weakens it. |
| Void Sovereign (`void_sovereign`) | Void Domain (2500): 10 blocks (12), 10 s (15): foes inside cannot teleport (pearls, Endermen, chorus fruit; commands are not affected), foreign projectiles are erased. Void Step (800): step up to 20 blocks to the last free spot, cutting the line (12 armour-ignoring damage). Collapse (3200): 12% of max health (min 20, boss a fifth) from every foe within 7 blocks, capped for players. |

## 4. Gaia in detail

* `GaiaZone` (pure record + registry): centre, radius, dimension, end time, owner; one zone per owner; cylinder (horizontal radius, vertical radius). Memory only (a restart ends domains).
* `GaiaCost`: raise price `2500 + 3 r^2`, upkeep per second `30 + 3 r + 4 x pillars in flight + 20 if blooming`, x1.5 on a war footing; never free, never negative.
* `GaiaWarCheck`: relation of a visitor to the owner from `EliteHooks.nationOf / atWar`: SELF, ALLY (same nation), ENEMY (both have a nation and are at war), NEUTRAL. Allies and the owner are blessed and may break blocks; enemies are burdened; everyone who is not SELF or ALLY is refused by the guard. With no nations system every visitor is NEUTRAL.
* `GaiaBlockGuard`: cancels `BlockEvent.BreakEvent` of a guarded visitor inside a live zone and removes the zone's blocks from `ExplosionEvent.Detonate` (unless the owner caused it). Config `gaia.blockGuard`.
* `GaiaBloomLedger`: points are earned (1 per second the owner stands in the zone, 5 per kill in it, cap 600), a vein costs 120 and is limited per game day (`gaia.dailyVeins`, default 8). Stored in the skill's own tag.
* `VeinBloom`: pure and seeded: ore weights (coal 40, copper 25, iron 25, lapis 5, redstone 5; gold 8 and diamond 2 only once mastered), 3-6 / 5-9 blocks, a connected random-walk shape, deepslate variants under y 0. `GaiaWorld.bloom` replaces only blocks in `#minecraft:stone_ore_replaceables` / `deepslate_ore_replaceables` inside the zone that the player may interact with.
* `GaiaPillars`: pure layout (3 or 5 pillars every 3.5 blocks, one rising every 4 ticks, damage building along the volley); `GaiaWorld.pillars` applies damage and a knock-up and sends the earth effects.

## 5. The evolution engine (`elite/skills/evolution`)

`evolutions.json` maps a skill id to the skills it can become, each with a list of requirements `{"trigger": "<id>", "params": {...}}`; **all** must hold. A path with an unknown trigger, a malformed entry or no requirement is skipped (and logged),
because a misspelt requirement would otherwise evolve for free. A data pack may replace the file or add its own copy; later packs win per (skill, target). `EvolutionTrigger` is one interface (`id`, `test`, `consume`, `describe`); triggers are pure and read an `EvolutionContext`
(the player's skills, EP, kills, items, awakening flags and the `EliteHooks` of the other subsystems), so they are unit-tested with a fake. `consume` (only `item_consume` pays) runs once after every requirement passed.

| Trigger id | Class | Params | Reads |
|---|---|---|---|
| `ability_mastered` | `AbilityMastered` | `skill?` (default: the evolving skill) | skill owned and mastered |
| `active_title_evolution` | `ActiveTitleEvolution` | `title` or `titles` | `EliteHooks.activeTitle` |
| `awakening_evolution` | `AwakeningEvolution` | `kind`: `any`, `demon_lord`, `hero` | player flag `nusmp_elite_awakened_<kind>` (set by `PlayerEvolutionContext.setAwakened`; no stable Tensura awakening API was found) |
| `ep_gate` | `EPGate` | `min` | the player's EP |
| `forge_crafted_count` | `ForgeCraftedCount` | `count` | `EliteHooks.forgeCraftedCount` |
| `forge_quality` | `ForgeQuality` | `min` (1-6 or common..mythical) | `EliteHooks.forgeBestQuality` |
| `has_title` | `HasTitle` | `title` | `EliteHooks.hasTitle` |
| `held_title_count` | `HeldTitleCount` | `count` | `EliteHooks.heldTitleCount` |
| `item_consume` | `ItemConsume` | `item`, `count` | carried items; takes them on success |
| `kill_evolution` | `KillEvolution` | `count`, `filter` (any, boss, monster, undead), `withSkill` (default true) | `KillTracker` counters in the player's data |
| `record_unlocked` | `RecordUnlocked` | `record` | `EliteHooks.recordUnlocked` |
| `reset_count` | `ResetCount` | `count` | `ResetLedger` / `EliteHooks.resetCount` |

**ResetCount is owned here.** `ResetLedger.record(player, reason)` counts a skill / class reset in the player's persistent data (`nusmp_elite_resets`), caches it by UUID, and `ResetLedger.install()` sets `EliteHooks.resetCount`. Ouroboros' Cycle records one; any other system that resets a skill or class should call `ResetLedger.record`.
`KillEvolution` counts kills per skill only while the skill is owned and only for skills that have a path; players, armour stands and your own pets never count.

`EvolutionRuntime` loads the table on every data reload, checks every player every 5 s (config) and, when a path is satisfied, pays the offerings, teaches the target (the old skill's data carried over like `SkillEvolution`), forgets the old skill if `evolution.replacesBase` (the existing `NUConfig` key) says so and announces it. The old `SkillEvolution` is unchanged and still runs, so both paths coexist (they cover different skills).

Shipped paths (`evolutions.json`), one per new Ultimate; together they use all twelve triggers:

| From | To | Requirements |
|---|---|---|
| `crocodile_skin` | `basilisk_skin` | mastered, 150 monster kills with it, 20,000 EP |
| `hephaestus` (the existing Unique) | `hephaestus_ultimate` | mastered, 50 items forged, Epic forge quality, offering of 4 netherite ingots |
| `scavenger` | `beelzebuth` | mastered, 400 kills with it, offering of 64 rotten flesh |
| `iron_resolve` | `gaia` | mastered, 3 titles held, 30,000 EP |
| `ancient_soul` | `ouroboros` | mastered, 3 resets, awakened |
| `meditation` | `rapheal` | mastered, record `lore_scholar`, active title `sage` / `scholar` / `lord_of_wisdom` |
| `void_edge` | `void_sovereign` | mastered, title `void_walker`, awakened as a demon lord, 100,000 EP |

The record and title ids are the names this subsystem *expects* the records and titles subsystem to define; until it does (or a data pack edits the file) those requirements do not pass, which is the safe default.

## 6. Config `nusmp-elite-skills-server.toml` (section `[elite_skills]`)

`enabled` (master switch; off: every skill, handler and evolution does nothing, skills stay registered); `[balance]` `damageScale`, `costScale`, `cooldownScale`, `kaiokenDamageScale`, `kaiokenDrainScale`, `meditationRegenScale`;
`[gaia]` `blockGuard`, `bloom`, `dailyVeins`, `pillars`; `[other]` `scavengerDrops`, `voidNullifyTeleport`, `voidNullifyProjectiles`, `ouroborosAutoRewind`; `[evolution]` `enabled`, `checkSeconds`, `broadcast`, `killTracking`.

## 7. Tests

* JUnit (pure, run with the project's shim here; 39 test methods in 4 classes): `EliteSkillRulesTest` (every table's relations), `GaiaRulesTest` (cost, zone, war check, ledger, vein, pillars), `EvolutionEngineTest` (all 12 triggers pass and fail on their own condition, ALL-requirements, offerings paid only when everything passed, broken data skipped, pack override, loose params, the shipped JSON parses and uses all 12 triggers), `EliteSkillAssetsTest` (21 ids, own 32 px icon, lang fragment complete, only Rukh and the mod's shapes, no Time Magic, one wiring line, doc present, `en_us.json` untouched).
* In-game self-tests (`/multiverse mega selftest elite.skills.<name>`, for the CI server smoke test): `registry`, `config`, `evolution_data`, `triggers`, `reset_ledger`, `evolution_flow` (a whole evolution with a fake player), `gaia_guard`, `ouroboros`, `iron_resolve`, `mechanical_hands`.

## 8. Owner rules

Nothing deleted or replaced (old skills, ids, icons, data stay). No version, CHANGELOG, README, LIBRARIES, gradle, mods.toml or `en_us.json` edit (lang goes to `tools/elite/skills_lang.json`). The Time Magic effects are untouched and unused. Weapon engravings (rule 6) do not apply: no weapon item is added (Void Edge is a skill; Runes and Temper write data onto weapons the player already has). New icons come from a new generator, 32x32, no two alike.

## 9. What is only type-checked, and what could be wrong

Nothing here was run in game (no Gradle, no Minecraft). Type-checked against API stubs (negative-controlled): everything under `elite/skills` and `skill/EliteBridge`. JUnit-run: the pure rules, Gaia parts, the trigger engine and the asset checks. Unverified in the real game:

* The Tensura `AspectualMagic` base: the three magics override the cast to the key RELEASE and do not use the Tensura cast-time pipeline; whether the real base class then shows them correctly in the magic UI, or needs a registered `AspectualType` flow, is unknown.
* Kaioken's rung: stored when the mode changes (`nextMode`) and when the press reports a mode; whether ManasCore reports the selected mode on a toggle press is not verifiable here.
* ManasCore damage hooks (`onTakenDamage`, `onDamageEntity`, `onDeath`) fire only for skills it treats as active; the upkeep and the damage multipliers that matter do not depend on them (game-bus handlers), but Ancient Soul's, Crocodile's, Basilisk's, Iron Resolve's, Rapheal's passives and Void Edge's second hit do.
* `EntityTeleportEvent` is cancelled for the pearl, Enderman and chorus subclasses; other mods' teleports are not covered. A glowing item lit by Scavenger stays lit if its owner logs out inside the 20 s (items despawn in 5 min anyway).
* The balance numbers were never played.
