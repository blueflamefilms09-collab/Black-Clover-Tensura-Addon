# Reference mods study: how other Tensura add-ons register and structure skills

Research only. Nothing was changed in Java, nothing is committed. The jars are untrusted data: they were listed with `unzip -l`, read with `unzip -p` (lang, tags, mixin json) and inspected with `javap -p -c -constants`. Nothing was decompiled and nothing was run.

**Labels used.** READ = seen in a listing, a json file, a constant or a signature. INFERRED = deduced from names, package layout or sizes; I did not read the method body. Source for names: the mods' own `en_us.json`. Only the Nightmares ultimate numbers also used its wiki (see `docs/ultimate_ranking_nightmares.md` sections 8 to 11); the rest is code study, not canon lookup. Owner rule: none of these mods may become a requirement of ours; they are read only to derive our own numbers and patterns. Files that could not be read are listed at the end.

## 0. The 16 jars at a glance

| Jar (mod id, version) | Classes | Skill-name lang keys | Skill registration style (READ) | Config style (READ) |
|---|---|---|---|---|
| `tensura-reincarnated-nightmares` (`trnightmare` 1.0.6.5) | 7,506 | 1,238 | ManasCore skills in `ability/skill/*` and `skill/ultimate`, `skill/unique`; `RegistrySupplier<ManasSkill>` | 185 config classes, per-skill subconfigs, `ManasConfig` API |
| `tensuramoreskills` (More Skills, 0.0.5.1) | 1,258 | 74 | `RegistrySupplier<ManasSkill>`, packages `skill/ultimate`, `skill/unique`, `skill/shades`, `skill/extra`, `skill/intrinsic` | `MoreSkillsConfigs`, `ClientVfxConfigs`, `EntityConfigs`, `MoreskillsRaceConfig` |
| `tensuramoreoverlord` (More Skills Overlord, 0.2.3) | 91 | 27 | `OverlordRegistries` + `registry/skills/OverlordDragonSkills` (`RegistrySupplier`, `register(String, Supplier)` helper, `init()`) | 5 config classes (`OverlordConfigInjector`, race configs, soul and wild magic configs) |
| `elite-tensura-mod` (`elitetensura` 1.0.2.2) | 1,405 | 38 | `ability/skill/{extra,intrinsic,ultimate,devTestingSkill}` | 25 config classes (`ETCalamityConfig`, `ETExtraSkillConfig`, ...) |
| `tensura-mysticism` (`mysticism` 2.2.1) | 933 | 62 | `ability/skill/{extra,intrinsic,unique}`, base class `MysticSkill` | `MysticismConfigs` + one config class per skill kind (`ExtraMysticismSkillConfig`, `IntrinsicMysticismSkillConfig`, `UniqueMysticismSkillConfig`) |
| `tensura-restricted` (`tensurarestricted` 1.0.0) | 129 | 0 | none: this is a quests / gacha / casino / auction mod (package `tensuraquests`) | `ModConfigs` + 12 config classes; `default_quests.json`, `default_gacha.json` |
| `devil-x-slime` (DxD, `dxdtensura` 0.2.107) | 2,405 | 37 | `DxDSkills`: Architectury `RegistrySupplier<XSkill>` plus a `ResourceLocation ..._ID` constant per skill | mixins only (2 json); lang 2,515 keys |
| `tensura-ascensions` (`ascension` 1.0.0) | 424 | 57 | `registry/AddonSkills` | none found (gamerule `doAscensionUltimate`) |
| `minesuraddons` (`tensura_minesura` 2.1.0) | 671 | 98 | `ability/skill/{common,extra,unique}` per-feature subpackages (`aegis/`, `artisanecho/`, ...) | `tensura_minesura/core`, admin commands |
| `tensura-blade` (`tensura_blade` 1.0.11) | 132 | 17 | `skill/DragonSkills`, `BlessingBearerSkill`, `PassiveBlessingSkill` | none found |
| `tensura-hardcore-ndc` (Hardcore Evolutions, 1.5.0) | 271 | 39 | `registry/EvolutionSkills`: typed `RegistrySupplier<ManasSkill>` with a `PENDENTES` list of deferred runnables | `EvolutionsConfig` (+ 48 config classes under `evolutions/config`) |
| `tensura-isekai-evolved-addon` (`trevolved` 2.0.3) | 436 | 51 | per-franchise packages `mushoku/skill`, `konosuba/skill`, `gravity/magic`, `time/magic` | 50 classes in `platform/config` incl. `ConfigReload`, `ConfigReloadNetwork`, `ConfigLayout` |
| `tensura-origins` (`trorigins` 2.0.5) | 257 | 7 | `registry/skill/{DragonFactorSkills,ExtraOriginsSkills,IntrinsicOriginsSkills,UltimateOriginsSkills}` with `register(String, Supplier)` and `init()` | `OriginsConfigs`, `DragonFactorSkillConfigs`, race configs |
| `tensuraleonboss` (`foxboss_leon_cromwell` 1.0.2) | 37 | 0 | none: a boss entity; `LeonCombatSkills` / `LeonSpiritronSkills` are attack routines, not registered ManasSkills (INFERRED from the lack of lang keys and registry) | `LeonConfig` |
| `date-a-slime` (`dateaslime` 0.0.1) | 257 | 4 | `ability/DateASlimeSkills`: Metatron, Michael, Zaphkiel, Camael | none found |
| `tensura-no-index` (`tensuranoindex` 1.3.7) | 6,216 | 288 | **compatibility only, see section 3** | `NoIndexConfig`, `NoIndexGameRules`, `ServerLoad`, `CompatRules` |

Note: `date-a-slime.jar` was in `/tmp/claude-0/refmods/` but not named in the task; it is included here.

---

## 1. Per mod: how it is built and what is worth adopting

### 1.1 TR Nightmares (`trnightmare`)
Huge (7.5k classes). Skills live in `ability/skill`, `ability/magic` (467 classes), `ability/battlewill`, with `skill/ultimate` (220) and `skill/unique` (171). Everything that is tunable goes through **185 config classes**: one subconfig per skill, with ManasCore's `ManasConfig` / `ManasSubConfig`. Content is grouped by `trnightmare_content_groups.json` so a whole family can be switched on or off. Tags (`data/tensura/tags`, 87 files) publish skill categories. Ultimate ranking details: `docs/ultimate_ranking_nightmares.md`.
Worth adopting: **(a)** per-skill config subobject with a `Default` fallback, so an owner can retune one skill without a release; **(b)** content groups (a datapack list of ids that a SERVER setting can disable together); **(c)** `ISpiritronAbility`, `INihilityAbility`, `IStardustAbility` style marker interfaces so a rule (a nullification, a counter) can test "is this a Nihility skill" without a list of ids. Not worth copying: 185 config classes for one mod, which is why the config folder is hard to read.

### 1.2 More Skills (`tensuramoreskills`) and its skill list
Packages `skill/ultimate` (20 classes), `skill/unique` (25 files, about 21 skills plus helpers), `skill/shades` (Naberius, Asmoday, Istaroth, Phainon, Ronova, Animated Shade), `skill/extra` (Morph, Purifier), `skill/intrinsic` (Symbiote, Anti-Symbiote, Black Communion, Blood Arts). Heavy client FX (220 classes under `client/fx`, per-skill packages `fx/avalon`, `fx/ronova`, `fx/arthur`, `fx/phainon`), 101 network message classes, and per-skill mixins (`ArthurAcquirementMixin`, `PhainonAcquirementMixin`, `EwigkeitMagicMixin`, `RonovaDeathDispatchMixin`, ...). Also three extra dimension types (`dimension`, `dimension_type`, `worldgen`) for domain skills.
Registration is `RegistrySupplier<ManasSkill>` in `TensuraMoreSkills` (7.9 KB). Mode names come from lang (see the Mode pattern below). The jar still ships the template skills `example_ultimate/common/extra/unique/intrinsic` (READ from lang: "Edit this description using the en_us.json file"): a leftover from the mod template.
Worth adopting: **Mode lang pattern** (each mode a lang key `skill.<id>.mode.<n>` so the radial menu and the lang tool see them), **acquirement mixins as separate small files** (one rule per file instead of a growing switch), and the **Ultimate evolves from a Unique** pairing (Hellfire to Vanir, Satella to Witch of Envy Satella, Fungus Infection to Abhoth). We already do the pairing in code (`SkillEvolution`).
Do not copy: the placeholders left in the lang (Aquilar "idk what do put here", "wip", Ewigkeit "Magik.").

**More Skills: every Ultimate and Unique, one line each** (names and mechanics are from the mod's own lang descriptions, READ; where the lang is a quote or a placeholder I say "no mechanics in lang" and the class name is the only evidence).

*Ultimate classes (`skill/ultimate`, 20):*

| Id (class) | One-line mechanics |
|---|---|
| `abhoth_lord_of_pestilence` (AbhothLordOfPestilenceSkill) | evolved Fungus Infection: the lord of fungal plague |
| `aquilar_lord_of_spacetime` (AquilarLordOfSpacetimeSkill) | space-time lord; lang says "wip" |
| `arthur_once_and_future_king` (ArthurOnceAndFutureKingSkill) | builds royal power in combat, rules holy and Battlewill damage, masters weapons, commands Excalibur |
| `avalon_god_of_eternal_kingdom` (AvalonGodOfEternalKingdomSkill) | multiplies holy/light/Battlewill/physical power, imposes Royal Laws, opens Avalon, commands the twelve Round Table knights |
| `axiom_lord_of_reflected_judgment` (AxiomLordOfReflectedJudgment) | the perfected evolution of Reflector |
| `caedros_god_of_conquest` (CaedrosGodOfConquestSkill) | ashen halberd; twists the world into a blood-red realm |
| `divine_general_mahoraga` (DivineGeneralMahoragaSkill + MahoragaAdaptationHandler) | King-Class: adapts to everything it survives |
| `elfaria_the_hallowed_icemaiden` (ElfariaTheHallowedIcemaidenSkill) | ice maiden; no mechanics in lang |
| `ewigkeit` (EwigkeitSkill) | Archivist of a Thousand Years; no mechanics in lang (mixins `EwigkeitMagic/Living/Mob/Detection` suggest it hooks spells, living entities and detection; INFERRED) |
| `kronairos_lord_of_the_endless` (KronairosLordOfTheEndlessSkill) | doubles EP gain, steals MP, weakens nearby foes, enforces Infinite Imprisonment |
| `mammon_casino` (MammonCasinoUltimateSkill) | earn Greed Chips on kills and gamble them for loot, power and a tiny chance at a new Ultimate |
| `michael_angel_of_locks` (MichaelAngelOfLocksSkill) | angelic key that devours prisons and suppressors and crowns space with divine locks |
| `pain_lord_of_six_paths` (PainLordOfSixPathsSkill) | Six Paths of Pain; no mechanics in lang |
| `schwi_ex_machina` (SchwiExMachinaSkill) | builds Calculation Stacks in combat to optimise damage and cooldown cycles |
| `true_authority_of_greed` (TrueAuthorityOfGreedSkill) | the true form of Authority of Greed: hoards its own fate, greed denies death its payment |
| `vanir_lord_of_hellfire` (VanirLordOfHellfireSkill) | evolved Hellfire: flames that burn strong and weak alike |
| `vanitas_king_of_injustice` (VanitasKingOfInjusticeSkill) | the weak are purged, the stronger punished, only one remains |
| `witch_of_envy_satella` (WitchOfEnvySatellaSkill) | evolved Satella: the half-elf that brought doom |
| `witch_of_vainglory` (WitchOfVainglorySkill) | "I always come back"; no mechanics in lang |
| (one more class, `MahoragaAdaptationHandler`, is a helper, not a skill) | |

*Ultimate ids in the lang whose class I did not locate in `skill/ultimate` (they may sit in `skill/shades` or another package; not checked):* `cursed_menagerie` (records slain beings into shadows; catalogue, parade, fusion, domain of shadow clones), `sovereign_of_the_great_tomb` (god-class undead dominion), `netherveil_overmind` (opens the Gate, spreads a Sculk infection, appoint controllers), `atomic_test_ultimate` "Atomic" (6 second compression then a world-cleaving beam), `samael_sovereign_of_wrathful_fate` (King fusion of Satanael and Susanoo), `chara_evolution_of_murderer` (forces a player into JUDGEMENT; strikes tear 20 % max health), `acedia_bushyasta_lord_of_eternal_stillness` "Dormiel" (stagnation and sloth), `first_king_of_history` (all treasures bow), `sheer_cold`, `high_pressure_shield`, `spatial_slasher`, `mori_equal_to_the_heavens`, `raging_radiation` (hold, charge 50,000 MP/s, Genesis-style beam), `incendiary_trap`, and the shades `naberius`, `ronova`, `istaroth`, `asmoday`, `phainon` (lang is a quote each).

*Unique classes (`skill/unique`):*

| Id (class) | One-line mechanics |
|---|---|
| `adaptability` | evolves through punishment (same display name as our `anime_multiverse:adaptability`, different namespace) |
| `albis_vina` | "I'm waiting for you, Will"; no mechanics in lang |
| `aquilar` | lang "idk what do put here" (placeholder) |
| `authority_of_greed` | a heart once claimed cannot escape desire; greed denies death its payment |
| `boogie_woogie` | swap positions with marked targets (Aoi Todo) |
| `chosen_king` | +1 % physical and Battlewill damage per mastered Battlewill; weapon master bonuses |
| `domain_of_negation` | a sphere that seals hostile abilities |
| `exploiter` | abuses loopholes in the world's rules |
| `fungus_infection` | studies, stores and weaponises hostile status effects |
| `gene_editor` | copies genes into syringes, stores extracted skills and stats, injects mobs |
| `godly_scientist` | absolute research |
| `godly_smithery` | absolute forging |
| `green_thumb` | plants ripen under your hand, gifts |
| `hellfire` | the burning dream: burn down everything that stands against you |
| `i_want_to_be_just_like_you` | drags the dying soul into limbo |
| `mitama_faith` | "Will you make a contract with me?" |
| `nirvana` | each fallen life becomes a karmic echo that awakens later |
| `parasite` | turns the user into a fragile parasite (intrinsic `symbiote` is the true form, `anti_symbiote` the remnant) |
| `pendragon` | Holy and Light damage x1.5 (x2 mastered), Blessed and Heavenly Eye; summon Excalibur for 500,000 MP |
| `satella` | "Aishteru"; no mechanics in lang |
| `void_archive` (VoidArchiveSkill, VoidArchiveForgeData) | no lang key found |
| `extermination_blade` (ExterminationBladeItem) | an item, not a skill |

*Other kinds:* Extra `morph` (copy the outer form of weaker beings, sneak to release), `purifier` (cleanses Black Blood infection). Intrinsic `blood_arts` (drain, Blood Surge, Crimson Guard, Kindred Rite), `black_communion` (final Bloodfiend intrinsic for the Crimson Progenitor), `symbiote`, `anti_symbiote`.

### 1.3 More Skills Overlord (`tensuramoreoverlord`)
The owner's add-on. Small (91 classes). 27 skill-name keys. Skills: Undead Body, Doomed Vanguard, Doom Knight Authority, Death Cavalier Charge, Twin Arcana, Ghoul King Hunger, Dragon Body (a 34 KB class), Dragon Lord Authority, Vampiric / Catastrophe / Heavenly Dragon Lord, Wight Lord Dominion (all `skill/intrinsic`). Registration: `OverlordRegistries` + `OverlordDragonSkills` with a private `register(String, Supplier)` helper returning `RegistrySupplier<E extends ManasSkill>` and a static `init()` called from the main class. Four mixins named `...WorldLaw...` (`MagicDualCastMixin`, `AbilityModeWorldLawMixin`, `MagicWorldLawMixin`, `ManasSkillInstanceWorldLawMixin`) touch `ManasSkillInstance`, so any mod that also mixes into it may interact (INFERRED; method bodies not read). It carries data folders for `ars_hex`, `ars_zero`, `enigmatic_arcana` (optional integrations).
Worth adopting: the tiny `register(String, Supplier)` + `init()` shape (we already have `book(...)` helpers). The "World Law" idea (a rule that rewrites mode behaviour for a whole world) maps to our `Tier` / class table and is a candidate for a gamerule.

### 1.4 Elite Tensura (`elitetensura`)
Biggest "game system" mod of the group: codex screens (29 classes under `screen/codex`), a **requirement framework** (`requirement/impl`, 43 implementations) and **record conditions** (`record/condition`, 30), 25 config classes, 125 loot tables, 28 enchantments, Patchouli books, UBS (bank) mixins, walpurgis, 9 mixins on Tensura (`MixinEnergyHelper`, `MixinTensuraSkill`, `MixinMagicUtils`, `MixinResetScrollItem`, ...). Skills: Extra (Ancient Soul, Iron Resolve, Meditation, Scavenger), Intrinsic (Kaioken, Mechanical Eye/Hands), Ultimate (Basilisk Skin, Beelzebuth, Gaia, Hephaestus, Ouroboros). It also keeps a `devTestingSkill` package (Brain).
Worth adopting: the **requirement registry with a Builder** and the datapack-driven titles and records read through a reload listener. That is the clean version of what our `SkillEvolution` and the proposed `UltimateGates` do in code. `docs/elite` already exists in our repo (our own Elite integration); this study is about the pattern, not our integration.
Clash notes: it has an Ultimate named `hephaestus`; ours is `anime_multiverse:hephaestus` (a God Unique). Different namespaces, but identical display names in the skill list. It also mixes into `EnergyHelper` (see No-Index below).

### 1.5 Tensura Mysticism (`mysticism`)
Race-heavy (`config/race` 85, `race/insect` 78, `race/direwolf` 42) plus skills in `ability/skill/{extra,intrinsic,unique}`, all deriving from `MysticSkill` (6 KB base). 62 skill-name keys, 55 `data/tensura/tags`, 42 worldgen files, 8 `biome_magicule` files. Config: **one config class per skill kind** (`ExtraMysticismSkillConfig`, `IntrinsicMysticismSkillConfig`, `UniqueMysticismSkillConfig`) plus `SpiritualMysticismMagicConfig`. 22 mixins into Tensura skills (`MixinFlameManipulationSkill`, `MixinGreedSkill`, `MixinPossessionSkill`, `MixinUltraspeedRegenerationSkill`, `MixinWaterManipulation...`): it patches existing Tensura skills to add behaviour rather than copying them.
Worth adopting: the **small base class `MysticSkill`** (shared tooltip, cost, mastery rules), which is the role our `NUSkill` plays. Also biome magicule data (data-driven world magicule) is an idea for dungeons.

### 1.6 Tensura Restricted (`tensurarestricted`)
Not a skill mod: package `tensuraquests` has `quest`, `gacha`, `casino`, `auction`, `shop`, `currency`, `goal` and 49 client classes. Data: `data/tensurarestricted/default_quests.json` and `default_gacha.json` (the defaults ship as json, configurable). Lang has 3 keys.
Worth adopting: **defaults in json, overridable by config**. Nothing for skills. Possible fit for our gold shop / Black Market, which is empty since the Skill Books were removed (`docs/skill_books_removed.md`).

### 1.7 Tensura DxD (`dxdtensura`)
Large (2,405 classes, 4,532 entries). Skills in `dxdtensura/skill` (323 classes) registered by `registry/DxDSkills` using Architectury `RegistrySupplier<XSkill>` and a public `ResourceLocation XXX_ID` constant for each, so other code references the id without touching the registry. Skills: Boosted Gear, Divine Dividing, Twilight Healing, Sword Birth, Forbidden Balor View, Absorption Line, Excalibur, Durandal, Evil Pieces, Brave Saints, ... 37 skill-name keys. 22 common mixins (`ReincarnationPoolFilterMixin`, `OtherworlderSkillDropMixin`, `FolgenSkillDropMixin`, `ReflectorPenetrateMixin`, `EliteTensuraRecordsMixin`: it hooks Elite's records) and 22 client mixins (flight poses, camera, `PhotonFxObjectMixin`, `TensuraKeybindsWheelMixin`). 379 `fx` json files and 109 meshes: effects are data files. Also has town, house, cutscene systems.
Worth adopting: **an `_ID` constant per skill** (so rules can compare ids without loading the registry holder) and **effects as data files** (`assets/<mod>/fx/*.json`). Risk to us: it adds `data/tensura/tags` (17 files) and mixes into `ReincarnationPoolFilter`; our Devil / Fairy Tail content is a different franchise, so no id clash, but both mods change reincarnation behaviour.

### 1.8 Tensura Ascension (`ascension`)
57 skill-name keys; `registry/AddonSkills` (21 KB); 99 client classes; 53 advancements (`data/ascension/advancement`); a custom `celestialmoons` dependency. The notable system is the **Awakening Altar** (see `docs/ultimate_ranking_nightmares.md` section 2). 26 mixins into Tensura (`MixinResistSkill`, `MixinDeathHandler`, `MixinMagicUtils`, `MixinReincarnationMenu`, race mixins).
Worth adopting: the **explain-a-failed-gate command** (`checkult`) and the **cooldown + gamerule** pair; both are tiny. The hard-coded pair table is the part not to copy.

### 1.9 Minesura Addons (`tensura_minesura`)
671 classes, 98 skill-name keys. Skills are grouped **per feature subpackage** under `ability/skill/{common,extra,unique}` (`aegis/` with Body, Heart, Last Spark; `artisanecho/`; `handicraft/`; `kineticbattery/`; `solarsynthesis/`), and features cover trading (31), faction (20), relic (20), race (cursed 17, soul 13), a bounty system (`stardetective/bounty` 17 + 14 network classes). 38 mixins, many tiny and **named for the exact rule they change** (`ChantAnnulmentCostMixin`, `HeavensExileCostMixin`, `CursedCircleCostMixin`, `GreedEpGainMixin`, `WrathBarrierBlockMixin`, `DuelMasteryBlockMixin`, `ClassExclusiveSkillMixin`, `MajinHolyLightResistBlockMixin`). 9 `weapon_attributes` json (Better Combat) and 17 `gear_existence` files. Admin commands that set caps (INFERRED from the earlier read; not re-checked in this pass).
Worth adopting: **feature-per-package skills** (a skill, its config, its mixin, its lang in one folder) and **one-rule-per-mixin naming**. Both make an audit like `docs/skill_audit.md` trivial.

### 1.10 Tensura Blade (`tensura_blade`)
Small (132 classes, 17 skill keys: `blessing_bearer`, `blessing_fire_avoidance`, `blessing_wind_spirit`, `blessing_earth`, `blessing_water_spirit`, `blessing_darkness`, ...). `DragonSkills` registers; `BlessingBearerSkill` is the parent and `PassiveBlessingSkill` the child: **one parent skill that grants and manages passive children**. 41 mixins on dragon behaviour (`DragonGearMixin`, `DragonCarrySyncMixin`, `Feldway*DragonGuardMixin`, `Reinhard*Mixin`). Data: `data/yigd` (death-chest compat) and `data/tensura_tno` (another add-on's namespace) tags.
Worth adopting: the **parent skill + passive children** shape for our grimoire-page and Beast Will passives, which today are modes and lists.

### 1.11 Hardcore Evolutions (`tensurahardcore_ndc`)
Almost no assets (322 entries); it is **rule patches**: 90 mixins named `Mixin<Race>_<aspect>` (`MixinChaosMetalloidRace_config`, `_reqs`, `_skills`, `MixinDevilLordRace_rotas`, `MixinDefaultRace_semHabilidade`). Skills in `skill/` (80 classes) named "<God/Lord> of X": Artemis, Atena, Azathoth, Baal, Beelzebuth, Buda, Dracula, Enmaa Daio, Eleleth, First True Dragon, Heimdall, Eitri, Turing, Kurniawan. `registry/EvolutionSkills` holds typed `RegistrySupplier<ManasSkill>` and a `PENDENTES` (pending) list of runnables: registrations are queued then run, to control order. `EvolutionsConfig` + 48 config classes under `config`. Mixes into the Plunder system (`MixinEntitySpawnHandler_unique`, `MixinRecurringSkillHandler_unique`). Uses `mysticism` as a dependency.
Worth adopting: **the "scaled ultimate" idea** (an ultimate is a multiplier set over the base skill, in config) and the per-aspect split of race patches (`_config`, `_reqs`, `_skills`): requirements, skills and config as three separate files per thing. Good match for the proposed `UltimateGates`.

### 1.12 Isekai Evolved (`trevolved`)
Franchise packages: `mushoku/skill` (52: Death God, Demon God, Dragon God, Dragon King, Education God, Quagmire, ...), `konosuba/skill` (11: Adventurer, Wizard, Crusader, Arch Priest, Explosion Goddess, Water Goddess, Fortress Goddess, Fortune God), `gravity/magic`, `time/magic`, `water/runtime` + `water/data`. **Config hot reload**: `ConfigReload` + `ConfigReloadNetwork` + `ConfigLayout` + `ConfigPresentation` + `CopyRulesConfig` + `CopyBlacklistMigration`: it re-reads config at runtime and syncs it to clients, and migrates old layouts (`ConfigLayoutMigrationMixin`). Server mode policy mixins (`ServerModePolicyMixin`, `ServerModeCostMixin`): the server decides which modes a skill may use and what they cost. `LaplaceFactor` attribute and `Overcharge` mixins show a held-charge pattern.
Worth adopting: **server-side mode policy** (we already say rule 9: server decides) and the **config layout migration** (we have many config files; renaming a key is a risk, see CLAUDE.md rule 12 `ConfigFileNamesTest`). Hot reload is nice but only for settings that are safe to change live.

### 1.13 Tensura Origins (`trorigins`)
257 classes, 1,645 entries (mostly recipes: 140 `data/tensura/recipe`, 154 `data/trorigins/recipe`, 134 `gear_existence`). 7 skill-name keys: Administrative Authority, Dragon Spirit Haki, and five Dragon Factors (Abyss, Earth, Frost, Scorch, Storm). `registry/skill` has four small files, one per skill kind (`Dragon`, `Extra`, `Intrinsic`, `Ultimate`), each `register(String, Supplier)` + `init()`. 54 race configs, 18 `sub_true_dragon` classes.
Worth adopting: nothing new for skills. Its "one registry file per skill type" is a tidy way to split our 281-line `NUSkills`.

### 1.14 Leon boss (`foxboss_leon_cromwell`)
37 classes, a GeckoLib boss entity `LeonEntity` (185 KB in the text dump) with `LeonCombatSkills` and `LeonSpiritronSkills` (attack routines inside the entity, about 44 KB each), `LeonLocomotion`, `LeonSpawning`. Config `LeonConfig`, a command, `data/.../function` (5 mcfunctions) and `entity_existence`. No ManasSkills.
Worth adopting: attack routines as a **separate class per attack family** instead of inside the entity; our boss code (`arc/midnight`, `boss/`) already splits this way.

### 1.15 Date a Slime (`dateaslime`)
Four skills in `ability/`: Metatron, Michael, Zaphkiel, Camael (angel Spirits, Date A Live), a boss package (37), `server` (46), custom shaders and `player_animations`. Registry `DateASlimeSkills`. Mixins: `LevelSetBlockMixin`, `SlotMayPlaceMixin`, client `TensuraStatusBarsMixin`, `MinecraftGlowMixin`.
Worth adopting: nothing structural. Possible candidate source for the angel theme in `docs/skill_audit.md` section 5 (not proposed there; it would overlap More Skills' Michael).

---

## 2. Cross-mod patterns, ranked by value to us

| Rank | Pattern | Seen in | Fit for us | Cost |
|---|---|---|---|---|
| 1 | **Requirement registry with a builder, data-driven titles/records, reload listener** | Elite, Nightmares (conditions), Hardcore (`_reqs`) | `UltimateGates`, `docs/ultimate_ranking_nightmares.md` item 4 | pure Java, no tick cost |
| 2 | **Per-skill config subobject with defaults** | Nightmares, Mysticism, Origins | owner can retune one skill without a release | one config section per skill; keep to the Ultimates only |
| 3 | **Explain-a-failed-gate command** | Ascension (`checkult`) | `/multiverse ultimate check` | on demand |
| 4 | **Mode names and descriptions all in lang** (`skill.<id>.mode.<n>`) | More Skills, Isekai Evolved | our `SkillText` already does names; check modes (see audit) | lang only |
| 5 | **Parent skill with passive children** | Blade | Beast Will and DB passives | medium |
| 6 | **Feature-per-package** (skill, config, mixin, lang together) | Minesura | new work only; no moves (rule 2) | none |
| 7 | **Config layout migration + hot reload of safe settings** | Isekai Evolved | rename-safe config; we already have `ConfigFileNamesTest` | medium |
| 8 | **One mixin per rule, named for the rule** | Minesura, Hardcore | audit-friendly; we have only 4 mixins | none |
| 9 | **Effects as data** (`fx/*.json`) | DxD, our own `VfxShape` | we draw from code; a data path would let the owner tune without a release | large; not now |
| 10 | **Scaled ("doubled") ultimates as config multipliers** | Hardcore | `UltimateClass` already has it as a table | none |
| 11 | **Content groups** (one switch for a family) | Nightmares | a SERVER list "disabled franchises" | small |

## 3. Tensura No-Index: compatibility only (not a pattern source)

`tensuranoindex` 1.3.7 is a 6,216-class mod (24,728 entries, 288 skill-name keys, 25k lang keys). I read it only for what it touches, so we can warn when it clashes with our ids and rules.

* **Our ids (`anime_multiverse:`): no clash.** Its tag files (`data/tensura/tags`: `ultimate_skills` 60 ids, `unique_skills` 82, `no_plundering` 16, `tome_copy_excluded` 16, `aspectual_magic` 13, `lightning_skills`, `wind_skills`, `engraving` 728) all use `"replace": false` (READ) and none of the ids contain `anime_multiverse` (READ). Our `engraving.json` is also `"replace": false`, so both merge.
* **Touch point 1: `EnergyHelperMixin`.** READ from the class constants: it targets Tensura's `EnergyHelper` and the method `isOutOfEnergy`. Our `NUSkill.charge` / `price` put the price where `getMagiculeCost` answers it and then call `EnergyHelper.isOutOfEnergy(player, instance, mode)` once (`docs/ultimate_classes.md` section 3). If No-Index rewrites that check, our class-scaled cost could be bypassed or doubled when both mods are loaded. **INFERRED; the mixin body was not read.** A self-test with No-Index present is the only way to know.
* **Touch point 2: `ManasSkillInstanceMixin`.** Targets `ManasSkill` and `ManasSkillInstance` (READ). Our milestone and mastery rescale (`NUSkill.rescaleMastery`, `restamp`) reads and writes instance mastery; a change to instance behaviour could shift it. INFERRED.
* **Touch point 3: `ExistencePoolCapMixin`, `SkillCreationMenuMixin`, `NightmareFaithMixin`, `NightmareAngelEffectsMixin`, `ScavengerDevourMixin`, `AscensionDevourMixin`, `AscensionHunterMixin`, `AscensionModifierMixin`.** It hooks the other add-ons on this list (Nightmares, Ascension, Elite's Scavenger). It is the only mod here that is written to run beside them; ours is not named in its mixins.
* **Same display names.** Its 288 skill names include `thelema`, `imagine_breaker`, `railgun`, `vector_control`, `meltdowner`, `dark_matter`. We removed `vector_manipulation` in 3.44.0 (`LegacyIds.REMOVED_SKILLS`), so no overlap now. Names are different namespaces anyway.
* **Config:** `NoIndexConfig`, `NoIndexGameRules`, `ServerLoad`, `CompatRules` (READ names). Its `NoIndexGameRules` could change evolution rules in the same world as our gamerule `skillEvolution`; names differ, so no clash.

## 4. Could not be read, or not verified

* The bodies of every method: nothing was decompiled. Rules such as "Mahoraga adapts" are from lang text only.
* Nightmares: the default essence cost values, the evolution condition bodies, and how the evolution menu spends essence.
* Minesura: the admin caps commands were taken from the earlier read and not re-checked in this pass.
* More Skills: the package of 14 Ultimate ids in the second list (`cursed_menagerie` ... `phainon`) was not located.
* Date a Slime, Blade, Leon, Origins: only listings, lang, registry signatures and mixin names; no skill bodies.
* Isekai Evolved's `gravity/magic` and `time/magic` skills were not listed id by id.
* Not fetched at all: no website, wiki or Fandom page (none was needed).
