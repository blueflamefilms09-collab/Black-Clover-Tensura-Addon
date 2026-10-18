# Elite races: the Saiyan line and the Primordial race

The owner's inventory line: "Races & Transformations: List all playable races (Saiyan tiers, Primordial race, base templates). Detail any transformation states or attribute
modifiers associated with them", with the templates `LesserSaiyan`, `MediumSaiyan`, `HighSaiyan`, `DivineSaiyan` and `PrimordialRace`. This is the WHY of everything in
`com.newuniverse.nusmp.elite.races`. **The original addon's source was not available, so every number below (stats, EP gates, durations, drains) is DESIGNED here**; they live in
`data/nusmp/elite/races/*.json` and are meant to be tuned.

## What was found (investigation)

**How Tensura Reincarnated models a race.** A race is a `ManasRace` in ManasCore's race registry (`RaceAPI.getRaceRegistry()`, key `RaceAPI.getRaceRegistryKey()`). Tensura's
`TensuraRace` adds the aura and magicule ranges, intrinsic skills, alignment, evolution requirements and the awakening / harvest-festival routes; `DefaultRace` adds a default
attribute block read from a `RaceConfig.Default` (twelve numbers: aura min and max, magicule min and max, size, max health, spiritual health, attack, attack speed, knockback
resistance, movement speed, swim speed). A player's race lives in `RaceAPI.getRaceFrom(entity)` (`Races`: `getRace`, `setRace`, `evolveRace`). Evolution is a list of
`EvolutionRequirement` objects with a progress each (Tensura's own: EP, items carried or consumed, boss kills, mastery of a skill, awakening, named); Tensura's evolution screen shows
them and `RaceHelper.evolveRace` changes the race. Race tags live in `data/tensura/tags/manascore_race/races/` (`divine`, `spiritual`, `can_glide`, `has_creative_flight`). A race's
icon is `textures/race/<id>.png`, 32x32. Resistances are Tensura SKILLS (`ResistanceSkills`), not attributes; the "ki" of a fighter is Tensura's AURA pool.

**Can a mod register a Tensura race? Yes, and this is how.** It is the same call this mod already makes for skills (`skill/NUSkills`): a NeoForge `DeferredRegister` created with
the ManasCore registry key (`DeferredRegister.create(RaceAPI.getRaceRegistryKey(), "nusmp")`) and registered on the mod bus. The owner's own uploaded Tensura add-on
(`tensuranoindex-0.71.0.jar`, the Aiwass races) is a working proof: its races extend `DefaultRace`, call `applyDefaultAttributeModifiers()` in their constructor after the stage is
assigned, give the stats through a small class that extends `RaceConfig.Default` and holds the twelve numbers (read from its bytecode as an API reference, nothing copied: its
stats are additive amounts, which fixes how the numbers here are read), override the evolution methods (`getDefaultEvolution`, `getNextEvolutions`,
`getPreviousEvolutions`, `getEvolutionRequirements`), add skills in `getIntrinsicSkills`, ship its tags under `data/tensura/tags/manascore_race/races/` and its icons under
`textures/race/`. So the five races here are REAL Tensura races, not buffs.

**How this repo already handled races** (all kept, none touched):

| Existing | What it is | Relation |
|---|---|---|
| `multiverse/Race`, `RaceBuffs`, `MultiverseProfile.race` | the mod no longer has own races beside Tensura's (0.27): a player's race is their Tensura race, mapped to a FAMILY by name (`Race.ofTensuraRace`), plus a small buff by family as transient modifiers; `DEVIL` is the one own race, command only | The old mechanism is a BUFF over a Tensura race. It cannot be an evolution line, has no stats block, no intrinsic skills and is invisible to Tensura's race command and evolution screen, which is why it was not used for this. See "interaction" below |
| `mana/Heritage` | the Black Clover kingdom and the blood of a Tensura race, buffs and debuffs | unrelated, stacks |
| `multiverse/CrownAwakening` | the Wizard King's awakening through `RaceHelper.evolveRace(p, evo, true, true)` | the same Tensura call the evolution service here uses |
| `avatar/race/*`, `data/nusmp/avatar_races/*.json` | races of AVATAR (NPC) bodies: ability scores, appearance, EP | another concept (a created creature, not a player race); the pattern of data files and a strict parser was followed |
| Tensura `ITransformationEffect` skills (Beast Transformation, Dragon Mode, Ogre Berserker) | transformations as SKILLS | nothing in the repo makes a race-level transformation state machine |

## NEW / EXISTS / REPLACE (owner rule 5)

| Part | Verdict | Note |
|---|---|---|
| Lesser, Medium, High, Divine Saiyan | **NEW** | nothing about Saiyans existed (grep) |
| Primordial race | **NEW** | |
| Race templates as data (`data/nusmp/elite/races/*.json`) | **NEW** | |
| Transformation states: base, ki boost, ascension, Great Ape, Divine | **NEW** | |
| Zenkai trait, kill counters | **NEW** | |
| Evolution rules (`RaceEvolutionRule`) and the evolution service | **NEW**, built on Tensura's `EvolutionRequirement` and `RaceHelper.evolveRace` | |
| `/multiverse elite race ...`, `nusmp-elite-races-server.toml` | **NEW** (a second branch of the existing `/multiverse` literal) | |
| `multiverse/RaceBuffs`, `Race`, `Heritage`, avatar races | **EXISTS, kept unchanged** | no replacement: nothing here does what the new races do |
| Kaioken skill | **not in this build** | the owner's inventory has a `KaiokenSkill` (the skills builder's); the ki boost coexists with it by id (see below), nothing is imported from it |

Nothing is a replacement; no existing code was removed or changed. The only edit outside the new package is the single wiring line in `elite/EliteSystem.java` (races section).

## The five races (template files)

Stats are Tensura's default-race block, additive on the vanilla base. Aura is the KI pool (Saiyans fight with aura; magicule is small), magicule is the magic pool.

| Race (class, id) | Tier | Aura | Magicule | Health | Spirit. | Attack | Atk speed | Knockback | Speed | Skills (natural first) |
|---|---|---|---|---|---|---|---|---|---|---|
| `LesserSaiyan` `lesser_saiyan` | 1 | 800 to 2,000 | 150 to 500 | +10 | 20 | +2.0 | +0.10 | 0.05 | +0.010 | Strength; Pain Resistance |
| `MediumSaiyan` `medium_saiyan` | 2 | 6,000 to 16,000 | 800 to 2,200 | +30 | 80 | +4.0 | +0.20 | 0.10 | +0.020 | Strengthen Body, Self-Regeneration; Pain, Physical Attack Resistance |
| `HighSaiyan` `high_saiyan` | 3 | 40,000 to 110,000 | 4,000 to 10,000 | +80 | 300 | +8.0 | +0.35 | 0.20 | +0.035 | Steel Strength, Body Armor, Self-Regeneration; Pain, Physical, Heat Resistance |
| `DivineSaiyan` `divine_saiyan` | 4 | 300,000 to 700,000 | 30,000 to 60,000 | +200 | 1,200 | +16.0 | +0.50 | 0.30 | +0.050 | Divine Ki Release, Thought Acceleration, Ultraspeed Regeneration, Body Armor; Pain, Physical, Magic, Heat Resistance |
| `PrimordialRace` `primordial` | 5 (own line) | 150,000 to 300,000 | 1,500,000 to 3,000,000 | +120 | 3,000 | +6.0 | +0.25 | 0.10 | +0.030 | Mana Manipulation, Magic Aura, Magic Sense; Magic, Spiritual Attack Resistance; fall damage halved, +4 safe fall |

The reasoning: each tier clears the previous tier's ceiling on aura and magicule, so a tier is a real step; the top Saiyan is far below the angels of the other add-on (3,000,000 magicule) while the
Primordial, a pure-mana being, has more magicule than any Saiyan by more than ten times and LESS aura than the top Saiyan (ki is the Saiyan's gift, mana the Primordial's). The Saiyan line
is one line (`line: saiyan`, tiers 1 to 4, each `evolves_to` the next); the Primordial is a line of its own at tier 5 (nothing leads to it, it leads nowhere). Tags: `divine_saiyan` is in
Tensura's `divine` tag and `primordial` in `spiritual`.

**EP gates were designed against the assumption that a race's EP follows its aura and magicule pools** (so a fresh Lesser Saiyan is near 1,000 to 2,500 EP, a Divine Saiyan 330,000 to 760,000):
each gate sits between a tier's EP and the next tier's, so training is needed. They are plain numbers in the files and a server dial scales all of them (`epGateScale`).

## Evolution (data-driven, one `RaceEvolutionRule` per requirement)

A race file lists `evolution` rules; each is a small pure class in `rule/EvolutionRules` with a progress 0..1, a weight (its share of the bar, normalised to 100) and a sentence.

| Rule | Reads | Used for |
|---|---|---|
| `ep` | the player's maximum EP, times `epGateScale` | every evolution |
| `kills` (`any`, `hostile`, `boss`, `player`) | the kill ledger (`runtime/RaceLedger`, fed by `LivingDeathEvent`; a creature below 10 max health counts for nothing, so a chicken farm cannot buy an evolution) | all |
| `mastery` | mastery of a skill, 0..1 (ManasCore `getMastery / getMaxMastery`) | the tier's natural skill |
| `item` (`consume`) | carried count; consumed only when the evolution really happens | enchanted golden apple (Medium to High), `nusmp:genesis_core` (High to Divine, and the Rite) |
| `record` | `EliteHooks.recordUnlocked(uuid, id)` (false until the records system exists) | `saiyan.legend` |
| `any_of` | the best child; only the child that was met commits | "10 boss kills OR the Saiyan Legend record", so the records system being absent cannot block an evolution |

| Step | Rules (share of the bar) |
|---|---|
| Lesser to Medium | EP 4,000 (50), 150 kills (30), Strength mastery 25% (20) |
| Medium to High | EP 30,000 (40), 3 boss kills or 600 kills (25), Strengthen Body mastery 50% (15), 1 enchanted golden apple, consumed (20) |
| High to Divine | EP 200,000 (40), 10 boss kills or record `saiyan.legend` (30), Steel Strength mastery 75% (15), 1 `nusmp:genesis_core`, consumed (15) |
| Primordial Rite (entry, from anywhere) | EP 100,000 (40), 1 `nusmp:genesis_core`, consumed (30), Mana Manipulation mastery 50% (20), 5 boss kills (10) |

Two ways in, one result. (1) Tensura's own evolution screen: the race returns one `EvolutionRequirement` per rule (`race/RuleRequirement`, green once met), Tensura changes the race
when the bar is full, and the old race's `onRaceEvolution` takes the items of the rules (once per tick, and only if every rule really was met, so an administrator's forced evolution keeps the player's items).
(2) `/multiverse elite race evolve <player> [force]` and the Rite `/multiverse elite race rite`, which check the whole set, change the race through `RaceHelper.evolveRace` / `setRace` and take the items themselves.

## Transformation states (a pure state machine)

`state/TransformMachine` is Minecraft-free: one machine per player holds the current state, when it ends and the rest of each state. States (`TransformState`, append-only): `base`, `boosted`,
`ascended`, `great_ape`, `divine`. A state is a `TransformSpec` read from the race file: `from` (entered from base or from a listed state), `min_tier`, `entries` (ANY one may hold; ALL conditions inside one entry
must: `ep_min`, `hp_below`, `moon` any / night / full_moon, `sky`, `item` + `consume`, `min_tier`; an entry may be `auto`), `duration_seconds`, `cooldown_seconds`, `aura_drain` (a share of MAX aura per second),
`ends_at_dawn`, `ki_scaled`, `rukh` and `color`, and `modifiers`. It ends when its time is up, at dawn (a night form), when the ki is spent, or when the race no longer has it (no rest then).

| State | Who | Entry | Lasts / rests | Upkeep | Modifiers (Lesser to Divine) |
|---|---|---|---|---|---|
| Ki Boost | all four Saiyans, Primordial ("Mana Surge") | at will | 2, 3, 4, 5 min / 30 s (Primordial 4 min / 60 s) | 0.6% aura/s (0.5% Divine) | attack +20, 30, 40, 50% of base, speed +10, 14, 18, 20% (Primordial +30% / +12%, plus magicule regeneration +0.5) |
| Ascension | Medium and up; Primordial ("True Form") | Saiyans: in a RAGE (hp at most 30% Medium, 50% High, 60% Divine, plus 15,000 / 90,000 / 300,000 EP: `auto`), High and Divine also at will (150,000 / 300,000 EP). Primordial: 400,000 EP | 4, 5, 7 min / 10, 10, 8 min | 1.0% (0.8% Divine) | attack +60, 80, 100%, speed +25, 30, 35%, health +10, 20, 30, armor +3, 4, 6, knockback +0.15 to 0.25 |
| Great Ape | all four Saiyans | the FULL MOON under the open sky (`auto`: it rises by itself), or a Power Ball (1 sea lantern, consumed, under the open sky), or at will from High (60,000 EP) and Divine (100,000 EP) | 4, 5, 6, 8 min / 10 min; ends at dawn | none | scale +100% (twice the size), attack +60, 80, 100, 120%, health +20, 30, 40, 60, armor +3 to 8, knockback +0.4 to 0.7, speed -15% to -5%, step height +1, reach +1.5. Dark Rukh |
| Divine Form | Divine Saiyan only (from ascension or the boost) | 500,000 EP | 2 min / 20 min | 1.2% | attack +150%, speed +50%, health +50, armor +10, knockback +0.4, ability mastery gain +0.5 |

All modifiers are TRANSIENT attribute modifiers with ids `nusmp:elite_state_<attribute>` and `nusmp:elite_zenkai_<attribute>` (never the base value): they are removed and re-added whenever the form,
the Zenkai stacks or the Kaioken overlap change, re-checked every five seconds, and rebuilt after a login or a respawn. The Great Ape's size is the vanilla `scale` attribute.

**Kaioken coexistence.** The race's ki boost and the mod's Kaioken skill (the skills builder's `KaiokenSkill`, expected at `nusmp:kaioken`, configurable) both multiply the body. The runtime only asks "is that skill toggled on?"
(nothing is imported from it). When both are on, a `ki_scaled` state's MULTIPLIERS on attack and speed (the two stats Kaioken touches) are scaled by `kaiokenOverlapFactor` (default 0.5); flat bonuses (health, armor, scale)
are never reduced, a non-ki state (Divine Form, the Primordial's) ignores Kaioken, and neither is switched off. Alone, each is at full strength. Until the skill exists the rule is inert.

**Zenkai** (trait of all four Saiyans, `state/Zenkai`, pure): a hit that leaves the player alive at or under 25% health adds a stack (once per 5 minutes, up to 3 per tier); each stack is +3% of base attack and +2 max health.

**Look.** The aura is the mod's own VFX (`VfxShape.SPIRIT_AURA` following the body, `SHADOW_UNITE` for the ape, a `LIGHT_FLARE` when a bright form begins) plus Light Rukh (ki, ascension, divine) or Dark Rukh (the ape) through `RukhFx`,
never a vanilla particle; no new client renderer. A line above the hotbar shows the form and the time left in the form's colour.

**Hotkey and commands.** One key (default `.`, rebindable, category Multiverse of Anime): press = toggle the best form (the ape is never chosen automatically), Shift + press = the next form in the cycle (boost, ascension, divine, ape), then back to base.
The client sends one of the words `auto`, `cycle`, `off`; the server decides (rate-limited to one per 5 ticks).

## Commands, config, files

`/multiverse elite race ...` (a second registration of the existing `/multiverse` literal; Brigadier merges them): `list`, `info [player]`, `set <player> <race>`, `transform <player> <state|off> [force]`, `evolve <player> [force]` (all permission level 2) and
`rite [race]` (open to everyone while `primordialRite` is on).

`nusmp-elite-races-server.toml`, `[races]`: `enabled` (the master switch: no transformation, no kill counting, no Zenkai, no evolution offered; the races stay registered because a registry cannot lose entries), `transformations`, `autoTriggers`, `greatApe`,
`greatApeAnyNight`, `zenkai`, `countKills`, `primordialRite`, `vfx`, `announce`, `epGateScale`, `durationScale`, `cooldownScale`, `drainScale`, `kaiokenOverlapFactor`, `kaiokenSkillIds`, `killMinHealth`.

Package `elite/races`: `EliteRaces` (the one registry class, `init(IEventBus, ModContainer)`), `EliteRacesConfig`, `RaceSelfTests`; `race/` (`EliteRace` extends Tensura's `DefaultRace`, `SaiyanRace`, the five template classes, `TemplateStats`, `RuleRequirement`);
`data/` (`RaceTemplate`, strict `RaceTemplateParser`, `RaceTemplates`, `RaceTemplateLoader`, `RaceAttributeKeys`); `rule/` (`RaceEvolutionRule`, `EvolutionRules`, `RaceRuleSet`, `RaceRuleParser`, `RaceCtx`, `KillKeys`); `state/` (`TransformMachine` and friends, `Zenkai`, `Coexistence`, `RaceEnglish`);
`runtime/` (`RaceTransforms`, `RaceEvolution`, `RaceRuntime`, `RaceLedger`, the two contexts, `RaceAttributes`, `RaceEnergy`, `RaceSkills`, `RaceAura`, `RaceMessages`); `net/`; `client/`; `cmd/`. Resources: five race files and `index.json`,
two tag files, five icons (`tools/elite/gen_race_icons.py`, deterministic, `--check`). Lang fragment: `tools/elite/races_lang.json` (not merged into `en_us.json`).

**When the files are read.** A Tensura race bakes its stats when it is built, at registration, before any world or datapack exists, so the bundled files are read from the mod's own resources then (`RaceTemplates.loadBundled`).
At world load and `/reload` the same files (or a datapack's replacements under `data/<ns>/elite/races/`, namespace `nusmp`) are read through the resource manager (`RaceTemplateLoader`): states, evolution rules and traits follow the datapack at once; the baked
stats need a restart. A bad file is logged and the bundled race stays.

## Interaction with `RaceBuffs` (for the lead)

`MultiverseProfile.race` falls back to HUMAN when a Tensura race maps to no family, so a Saiyan or a Primordial also gets the Human family buff (+5% attack, +1 luck). It is harmless but unintended. One added line in `Race.ofTensuraRace`
(for instance `if (p.contains("saiyan")) return OGRE; if (p.contains("primordial")) return UNDEAD;`) would give them a family of their own. It was not made here (the rules say to add, not to change, and the file belongs to the multiverse system).

## Tests

JUnit (pure, no Minecraft): `RaceEvolutionRuleTest` (every rule, the weighted bar, all-or-nothing commit, `any_of`, the strict parser), `TransformMachineTest` (entry, `from`, EP / health / moon / sky / item conditions, duration, rest, dawn, ki upkeep, REMOVED, auto triggers,
hotkey order, save round trip and corrupt saves, Kaioken scaling, Zenkai), `BundledRaceTemplatesTest` (reads the real shipped files: line structure, growth by tier, gates, reachable states, the ape is moon-bound, Divine is the top tier's, the Primordial stands apart, and the parser's refusals),
`RaceTemplatesTest` (a bad datapack file keeps the bundled race, a good one applies, the line check), `RaceLangFragmentTest` (the fragment equals the code's English; every message key is in it). In-game self-tests (`/multiverse mega selftest`, add the prefix `elite.races` to the smoke script's expected list):
`elite.races.registry`, `.templates`, `.race`, `.transform`, `.evolve`, `.coexist`.

## Honest limits: what is only type-checked

Everything here was type-checked against the API stubs and the pure layer was run as JUnit; the Gradle build, the game, Tensura's real race code and the client were NOT run. Points that depend on Tensura's unseen internals and are the first things to check in game (each is covered by a self-test where it can be):
1. the stats are read as additive amounts and `applyDefaultAttributeModifiers()` is called after the template id is set (copied from the working add-on's pattern);
2. `getEvolutionRequirements` weights add to 100 with each progress 0..1 (the add-on's single requirement uses weight 100); whether Tensura's screen needs more than that;
3. `setRace(race, true)` is "set and reset the existence data"; `RaceHelper.evolveRace(p, race, true, true)` is the call `CrownAwakening` already uses;
4. `onRaceEvolution(old, entity, new)` is called on the OLD race when a player evolves;
5. the registry names of the Tensura skills (`tensura:strength`, `strengthen_body`, `steel_strength`, `divine_ki_release`, `mana_manipulation` ...) are the lower-case field names; `elite.races.templates` lists any that do not resolve;
6. the `divine` and `spiritual` tag semantics in Tensura; the vanilla `scale` attribute on a player (camera, hitbox);
7. the hotkey, the HUD line and the aura were never seen on a screen; the self-tests drive a stand-in player (`FakePlayer`) with no connection;
8. `nusmp:kaioken` is a guess at the id of a skill that is not in this build; the records hook `saiyan.legend` has no owner yet (the `any_of` keeps the evolution reachable without it).
