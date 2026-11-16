# How TR Nightmares ranks Ultimate skills, how Tensura Ascension awakens them, and what we should take

Research only. No Java was changed, nothing is committed, the version is not bumped. This file proposes; it builds nothing.

**How this was read (honest scope).** The reference jars are untrusted and nothing was decompiled. Every statement below is one of:

* **READ** = read from `unzip -p` (lang json, data json, tag json) or from `javap -p -c -constants` output (field names, constants, string literals, method signatures).
* **INFERRED** = deduced from names and call shapes. Where a rule's exact arithmetic would need the bytecode body, I did not follow it and I say so.

Nothing from the reference mods was run. Sections 1 to 7 are code study from the jars. Sections 8 to 11 (class scaling) add the Tensura Nightmares wiki (English, a local copy; CLAUDE.md rule 10) and a cross-mod comparison; what could not be fetched is listed in section 11. The reference mods stay data only: none becomes a dependency.

Mods covered: TR Nightmares (`trnightmare`, 1.0.6.5), Tensura Ascension (`ascension`, 1.0.0). Cross-reads: More Skills (for the "Ultimate" shape), Hardcore Evolutions (scaled ultimates), No-Index (what it touches).

---

## 1. TR Nightmares: the whole ultimate pipeline

### 1.1 What an "Ultimate" is (READ)

* Base class `ability/skill/ultimate/UltimateSkill extends Tensura Skill` (abstract). Constructor takes a `Skill.SkillType`. It adds `DefaultConfig()` (abstract, returns a `NightmareUltSkillConfig$Default`), `getAcquirementMastery(entity)`, `getMaxMastery()` and `getDefaultAcquiringMagiculeCost()`. So every Nightmares ultimate carries its own config object, and the cost to acquire and the max mastery both come from config, not from a constant.
* About 220 classes sit in `skill/ultimate/` and about 170 in `skill/unique/` (package counts from the jar listing). The tag `data/tensura/tags/manascore_skill/skills/ultimate_skills.json` lists **78 ids**; `unique_skills.json` lists **79 ids**. The tags are how Tensura and other mods recognise "this is an ultimate" regardless of class.

### 1.2 Tiers, ranks, classes (READ)

The word "class" in Nightmares is a **flavour class**, not a power ladder. `NightmareUltSkillConfig$UltMastery` has seven fields, one per class:

| Field | Class | Default max mastery (READ from the constructor constants; assignment order INFERRED from the field order) |
|---|---|---|
| `masterySinUlt` | Sin (Seven Deadly Sins style) | 15,000 |
| `masteryVirtueUlt` | Virtue | 25,000 |
| `masteryAngelicUlt` | Angelic | 5,000 |
| `masteryDemonicUlt` | Demonic | 5,000 |
| `masteryGodUlt` | God | 50,000 |
| `masteryKingUlt` | King | 35,000 |
| `masteryGenericUlt` | Generic | 5,000 |

So rank is expressed through **how long it takes to master** (max mastery), plus the EP/essence price to evolve into it. God (50,000) > King (35,000) > Virtue (25,000) > Sin (15,000) > the rest (5,000).

The per-skill config (`config/ability/skill/NightmareUltSkillConfig*`, about 100 per-skill subconfigs, file path `nightmare/ability/skill/nightmare_ult`) lets each ultimate override: its class mastery, `mpAcquirement` (magicule cost to acquire) and `enableUltimateEvolution` (a switch). Example subclass read: `AsmodeusSkill` has its own config class.

### 1.3 Costs and the "essence" price (READ names, INFERRED meaning)

`config/NightmareUltimateConfig` holds about **100 int fields** named `<Skill><Kind>Cost`, for example `AsmodeusSoulCost`, `AsmodeusElderCost`, `AsmodeusLifeCost`, `BaelDemonCost`, `CamaelHolyCost`, `CthulhuEnderCost`, `DeusExHihiroCost`, `EscanorBlazeCost`, `JibrilElementalCost`, `BanAppleCost`, `HubrisBloodCost`. The suffixes (Soul, Elder, Ender, Demon, Holy, Blaze, Life, Elemental, Blood, Apple, Hihiro) read as **essence types**: each ultimate costs a number of a specific essence kind per evolution path. I did not read how an essence is stored (inferred: items or a counter in the evolution menu). Default values were not dumped.

### 1.4 How a player gets one: code-driven evolution, not data (READ)

* `ability/evolution/EvolutionConditions` builds, per skill, a **list of `EvolutionCondition`**. About **60 condition types** exist as classes (`EvolutionCondition*`): they read as "has skill X mastered", "killed N of mob M", "has title T", "max EP at least N", "is race R", "consumed item I", and so on (names INFERRED from class names; bodies not read).
* `UltimateEvolutionHelper.tryEvolveFromUnique` is the entry point from a Unique to an Ultimate. `SkillEvolutionHelper` is the generic version for non-ultimate lines.
* So the **ultimate** rules are Java. The data-driven part (`data/trnightmare/skill_evolution/*.json`, **16 files**) is only for ordinary "line" evolutions, not the ultimate ladder. Other data: `trnightmare_racial_progression/*.json` (45), `trnightmare_titles`, `trnightmare_content_groups.json` (groups content so a pack can switch a whole family on or off).

### 1.5 Slots, the single-owner rule, game rules (READ)

* `util/UltimateSlotHelper` plus `config/mechanic/UltimateSlotConfig`: the number of Ultimates a player can hold is **capped**. A pending queue exists: when the cap is full, a new ultimate waits until a slot is free (INFERRED from "pending queue" in the class and menu names).
* `feature/dragon/TrulyGodClassOwnership`: a `SavedData` that stores **one** owner of the "truly god class" ultimate for the whole world. So the top rank is unique per server (READ: class exists; INFERRED: semantics from name).
* `world/TRNightmareGamerules`: gamerules that switch evolution and acquisition on and off per world.
* An evolution menu and screen exist (`trnightmare/menu`, `client/screen`) so the player picks the path and pays the essence.

### 1.6 Nullification and resistance (INFERRED, not followed)

Mixins such as `AntiSkillMixin`, `CreatorSkillAntiCountMixin`, `DisintegrationMixin` suggest ultimates interact with anti-skill and skill counting. I did not read their bodies. I cannot say how Nightmares ranks one ultimate against another's nullification.

### 1.7 Config files (READ)

Two SERVER-style spec classes: `NightmareUltimateConfig` (the essence costs) and `NightmareUltSkillConfig` (per-skill mastery, acquirement, evolution switch). They use ManasCore's config API (`ManasConfig` / `ManasSubConfig`) for the per-skill part and NeoForge `ModConfigSpec` for the costs.

---

## 2. Tensura Ascension: Unique to Ultimate by ritual

### 2.1 What it does (READ from `handler/AwakeningRitual` constants, `registry/AddonSkills`, and the lang file)

* A **multiblock Awakening Altar** plus a throwable **Ultimate Catalyst** item. Throw the catalyst on the altar, right-click, and you receive "the Ultimate matching whichever Unique you have mastered".
* A static table `AwakeningRitual.AWAKENINGS` holds about **10 hard-coded Unique to Ultimate pairs**. The ids visible in string constants: `timeless_mage`, `one_who_seals`, `slayer_of_dragons`, `evil_majin`, `unbound_jester`, `apocalypse_bird` (and Punishing Bird / Judgement Bird for the "all three birds" rule).
* **Universal gates** (all READ from lang keys and constants): the Unique must be **mastered**; the player must be a **True Demon Lord or True Hero** (`requires_status`); **5,000,000 Max EP** (`requires_ep`); a **per-player cooldown** (`ascension:last_awakening_tick`); the gamerule **`doAscensionUltimate`** (default true).
* **Per-ultimate tribute gates** (lang): Timeless Mage needs every aspectual magic mastered; Sealer needs a Suppression Stone holding 400,000 Max EP; Slayer of Dragons needs 30 Dragon Essences; Evil Majin needs 50 Majin Candies eaten (counter `ascension:majin_candies_eaten`); Unbound Jester needs 1,000,000 spirit damage dealt while shackled (counter `ascension:jester_spirit_damage`); the Black Forest line needs all three Birds mastered.
* Admin and helper commands: `checkult` (explains which gate is unmet, in plain text), `forceultawaken` (stages the cutscene at a nearby altar), cooldown reset, a codex awaken helper.
* **Witness bonus**: bystanders at the altar get a Max EP bonus ("You witnessed X's awakening").

### 2.2 Why it matters for us

Ascension has no class ladder at all. Its rank signal is the **gate stack** (status + EP + tribute). The thing worth taking is the shape: a visible, explainable checklist (`checkult`) and a cooldown, with a world rule to turn it off.

---

## 3. What we have today (READ from our source)

| Piece | Where | What it does |
|---|---|---|
| Class table | `skill/rules/UltimateClass` (enum LORD, KING, GOD, DRAGON) | per class: `epCap` (damage ceiling of the EP scaling) 8/10/12/12, `costPct` 2/3/5/4 %, `costRef` 2,000/3,000/4,000/3,000, `maxMastery` 10,000/25,000/50,000/15,000, `killBelow` 0/0/0.60/0, `shredBelow` 0/0.70/0.80/0. Server-configurable, clamped. `killBelow` and `shredBelow` are data only (no skill reads them yet). |
| Base class | `skill/NUSkill` | `ultimateClass()` (null for everything not classified), `getMaxMastery`, `tier()`, `price/pay/charge` through Tensura `EnergyHelper.isOutOfEnergy`, milestones at 25/50/75/100 %, `rescaleMastery` |
| Tier | `skill/rules/Tier` | BASE, MASTERED, ULTIMATE, ULTIMATE_MASTERED |
| Evolution | `skill/SkillEvolution` | 7 hard-coded Unique to Ultimate pairs, checked every 100 ticks per player; gate = mastered base skill + `ultimate_evolution.minimumEP` (default 0) + gamerule `skillEvolution` + config `evolutionReplacesBase`. Then `DbSkills.evolve` (15 pairs) and `BeastSkills.evolve` (27 pairs) with their own tables. |
| Classified skills | 15 classes override `ultimateClass()` | GOD: The Almighty, The Visionary, All Fiction. KING: Gate of Babylon, Hell Conqueror, Lord of Summons, Goddess of Faith. LORD: Adolla Burst, Black Wings, Hero's Argonaut, Immortal Will, Ki, Nen, Shishou, Lord of Gluttonous Greed; also every Dragon Ball and Beast Will Ultimate (LORD, through `DbSkill` / `BeastSkill`). DRAGON: nobody yet. |
| Unclassified Ultimates | Veldanava five (`entity/veldanava/skills/VeldanavaSkillBase extends Skill`) | `SkillType.ULTIMATE` but not NUSkill: own costs, own mastery, **no class**. Obtained only by admin command (`/multiverse ultimate give`; the boss and card screen were removed in 3.41.0). |
| Separate system | `ultimate/` (UltimateDef, UltimateRegistry, UltimateRules, UltimateGate, UltimateConfig, UltimateManager, UltimateDefs1..4) | "Ultimate Magic" grimoire pages. A different thing from the class ranking above. Two systems share the word "ultimate". |
| Config | `NUConfig` `[ultimate_evolution]` and `[ultimateClasses]` | SERVER |
| Docs | `docs/ultimate_classes.md`, `docs/MASTER_GUIDE*` (owner's build guide, Part 5 section 5.2) | design and settings |
| Tests | `skill/UltimateClassSelfTests`, `UltimatePairSelfTests` | class of each skill is asserted, pairs asserted |
| No data layer | `src/main/resources/data/tensura` has only `tags/enchantment/engraving.json` | we publish **no** `manascore_skill/skills/*_skills.json` tag |

---

## 4. Side by side

| Question | TR Nightmares | Tensura Ascension | Ours |
|---|---|---|---|
| Rank signal | 7 flavour classes with different max mastery (5,000 to 50,000) | none; gates only | 4 power classes with EP cap, cost percent, max mastery |
| Where defined | Java class per skill + per-skill config subobject | Java static table | Java enum + one override line per skill |
| Data-driven? | Only non-ultimate lines (16 json); ultimates are code | No | No |
| Skill tag for other mods | Yes, `ultimate_skills.json` (78) and `unique_skills.json` (79) | Registers via its own registry | **No tags published** |
| Unique to Ultimate | `tryEvolveFromUnique`, about 60 condition types, essence cost per path | altar ritual, 10 pairs, EP 5M + status + tribute | 7 + 15 + 27 pairs in three tables; gate = mastered (+ EP floor default 0) |
| Cost to cast | `mpAcquirement` per skill (acquire) | not class based | `max(flat, maxMagicule x costPct x flat / costRef)` per class |
| Mastery length | by class | by Tensura default | by class (`rescaleMastery`) |
| Slot cap | yes, plus pending queue | no | **none** |
| One-owner top rank | yes (`TrulyGodClassOwnership`) | no | none |
| World switch | gamerules | `doAscensionUltimate` | gamerule `skillEvolution` (all-or-nothing) |
| Explain a failed gate | evolution menu shows it | `checkult` command | only a rare chat hint about EP |
| Admin tools | commands (inferred) | `checkult`, `forceultawaken`, cooldown reset | `/multiverse dragonball|beastskill give` for DB and Beast only |
| Per-skill override without code | yes (per-skill config) | no | class table only; moving a skill = code edit |

## 5. Gaps in our system (the facts behind the proposal)

1. **Five Veldanava Ultimates have no class**, so the class math (cost percent, mastery length, EP cap) never touches them. They are the GOD tier by the guide but are outside it.
2. **`DRAGON` is an empty class.** The enum is ready; no skill uses it.
3. **Three separate evolution tables** (`SkillEvolution` 7, `DbSkills` 15, `BeastSkills` 27) each with their own check. A new Ultimate means picking the right table or adding a fourth.
4. **The evolution gate is only "mastered"**, because `minimumEP` defaults to 0. A fresh Ultimate holder can have a few thousand magicule. The class's `epCap` and cost percent are tuned for late game, so rank has no entry price.
5. **No per-class entry gate.** Ascension's EP + status gate and Nightmares' essence price both make an Ultimate a milestone. Ours is a by-product of mastering.
6. **No `checkult`-style explanation** of what is missing.
7. **No slot cap and no one-owner rule.** With several players on a server, every player can hold every Ultimate.
8. **No skill tags.** Tensura and other mods that read `ultimate_skills` (No-Index adds to `tensura:ultimate_skills` / `unique_skills`; Nightmares counts and filters by them, INFERRED) never see ours. This also means we do not clash, but we are invisible to them.
9. **Moving a skill between classes needs a code edit.** The owner said "the owner can move any skill" (docs/ultimate_classes.md); today that costs a release.
10. **`killBelow` and `shredBelow` are not read by any skill** (docs/ultimate_classes.md says so); the GOD class advertises 60 % and 80 % thresholds that do nothing.

## 6. Proposal (a list of changes by file; none are made)

All of it follows CLAUDE.md rule 2 (add, keep the old), rule 5 (everything below is NEW unless marked), rule 9 (server decides), rule 11 (bounded). Each item has a setting that restores today's behaviour.

| # | Change | File(s) | NEW or REPLACEMENT |
|---|---|---|---|
| 1 | A pure **rank lookup keyed by skill id**, not by inheritance: `UltimateRanks.of(ResourceLocation)` returns the class. Seeded from the 15 `ultimateClass()` overrides plus the five Veldanava ids as GOD. Skills that are not NUSkill (Veldanava, grimoire-based) get a class without being rewritten. | new `skill/rules/UltimateRanks.java` (pure, unit-testable) | NEW |
| 2 | Make `SkillDamage` / `NUSkill.pay` fall back to `UltimateRanks` when `ultimateClass()` is null. | `skill/NUSkill.java`, `skill/SkillDamage.java` (append a fallback line) | NEW behaviour behind a setting `useRankTable`, default true |
| 3 | **One pair table** that the three evolution checks read: `UltimatePairs` holds (base id, ultimate id, gate list). The old three tables stay and are loaded into it, so nothing is removed. | new `skill/rules/UltimatePairs.java`; `SkillEvolution.java` calls it first, old tables stay | NEW; the old tables KEPT |
| 4 | A small **gate record** modelled on Nightmares' `EvolutionCondition` list and Ascension's `checkult`: `Gate.mastered`, `Gate.minMaxEp(class)`, `Gate.status`, `Gate.tribute(item, n)`. Pure, so a unit test reads every gate. Default per-class EP floors, all SERVER settings: LORD 0, KING 0, GOD 0 (so today's play is unchanged until the owner raises them). | new `skill/rules/UltimateGates.java`; `NUConfig.java` append `[ultimateClasses.gates]` | NEW |
| 5 | **`/multiverse ultimate check [player] [skill]`**: prints which gate is unmet. Server-side only, permission 0 for self and 2 for others. | new `skill/UltimateCheckCommand.java` | NEW |
| 6 | **Ultimate slot cap** (default off = unlimited = today). Per-player UUID, stored in the existing player data, no per-tick work. | new `skill/UltimateSlots.java`; `NUConfig.java` append `maxUltimates` | NEW |
| 7 | **Optional one-owner top rank** for GOD (Nightmares' `TrulyGodClassOwnership` idea). Off by default. A `SavedData` keyed by UUID; released on logout-timeout, death and when the owner forgets the skill. | new `skill/GodOwnership.java` | NEW, owner decision |
| 8 | **Per-skill class override in config** so the owner moves a skill without a release: `[ultimateClasses.perSkill]` map id to class name. Falls back to the code table. | `NUConfig.java`, `UltimateRanks.java` | NEW |
| 9 | **Publish skill tags** `data/tensura/tags/manascore_skill/skills/ultimate_skills.json` and `unique_skills.json` listing our ids. This changes how Tensura and other mods treat our skills (counting, nullification, loot filters), so it is a **decision for the owner**, not a default. If chosen, keep the files generated by a tool so they cannot drift from the registry. | new data files + a generator under `tools/` | NEW, owner decision |
| 10 | Fill the `DRAGON` class (Black Clover devil track, per the existing enum comment), or confirm it stays empty. | skill files only; enum untouched | owner decision |
| 11 | Wire `killBelow` / `shredBelow` or remove them from the docs table (they advertise something that does not happen). | `docs/ultimate_classes.md` correction; code only if wired | owner decision |
| 12 | Tests: extend `UltimateClassSelfTests` with "every registered ULTIMATE has a rank"; add a pure `UltimateGatesTest`; add the new prefixes to the CI smoke list (CLAUDE.md rule 12). | `src/test`, `UltimateClassSelfTests` | NEW |
| 13 | Keep the **second "ultimate" system** (`ultimate/`, grimoire pages) clearly named in docs so the two are not confused. No code change. | docs | NEW doc line |

**Worst-case cost (rule 11).** Nothing runs per tick. The pair check stays on the existing 100-tick cadence per player: at most about 49 pairs (7 + 15 + 27) and, with gates, a few attribute reads each, so at most 49 lookups every 5 seconds per player. `UltimateRanks.of` is a map lookup. The `check` command is on demand. Slots and ownership are touched only when a skill is learned or forgotten. No new packets: the `check` result is one chat message to the asker. Client cost: none.

**Not checked.** I did not read the Nightmares evolution condition bodies, the default values of the essence costs, how the evolution menu spends essence, or how its mixins decide anti-skill counts. If the owner wants the essence-price idea (which we do not have), that needs a second, deeper read of `EvolutionCondition*` and the menu classes.

## 7. What I would not copy

* Nightmares' seven flavour classes. Our four power classes are simpler and already tuned in the master guide.
* Nightmares' 100 essence-cost ints. They need an essence economy we do not have.
* Ascension's hard-coded static table. It is the weakest part of that mod; ours should stay a registry plus config.

---

## 8. Class-by-class scaling, Lord up to Dragon (owner request)

**Sources, and how much to trust each.**

* **Wiki (READ, local copy).** The Tensura Nightmares wiki (`tensuranightmares.wiki.gg`, English) was reachable only through `curl` on its MediaWiki API; the coordinator saved the wikitext in `/tmp/claude-0/tn_wiki/pages/` (142 pages: 71 unique, 50 lord, 11 king, 6 god, 4 dragon). I parsed only the header fields (`Type`, `Obtain Cost`, `Points to Master`, `Other` requirements) and counted the `*[Passive` and `*[Active` bullet lines. No text was copied. Page names used are in 8.5. `fandom.com` and `trmysticism.wiki.gg` do not resolve from this session (section 11).
* **Jar (READ, javap).** `NightmareUltSkillConfig$<Skill>` constants (`mpAcquirement`, EP requirements, numbers per ability), the `UltMastery` defaults, and `getModes()` constants of each `ability/skill/ultimate/*Skill`.
* The wiki's tier labels (Lord / King / God / Dragon) are the **community labels** in the page headers ("King Class Ultimate Skill", "God Class Ultimate Skill", "Dragon Class Ultimate Skill"); the jar's own grouping is by mastery category (Sin / Virtue / Angelic / Demonic / God / King / Generic). They overlap but are not the same list; 8.3 maps one to the other.
* Passive and active counts are bullet counts on the wiki page, so a page that lists fewer abilities than the mod has reads low. Jar mode counts (`getModes`) count wheel modes, including toggles and sub-modes; the two do not match one to one.

### 8.1 The table (Nightmares, by wiki tier)

| Tier | Pages | Obtain cost (MP), min / median / max | Extra gate (EP) | Points to master | Passives per skill (median, range) | Actives per skill (median, range) | Jar: max mastery default |
|---|---|---|---|---|---|---|---|
| Unique (the step before) | 71 | 40,000 / 75,000 / 1,000,000 (38 pages state a cost) | none stated | 1,000 (up to 2,500) | 1 (0 to 3) | 2.5 (1 to 8) | n/a (Unique) |
| **Lord** (wiki category "Ultimates") | 50 | 120,000 / 900,000 / 5,000,000 (39 stated) | a few: Hestia 2M, Gilgamesh 2M, Galatine 3M plus True Hero, Solomon 12M, Acnologia 500k | 5,000 (a few higher: Asmodeus 15,000; Alternative, Michael, Sariel, Yog-Sothoth 25,000) | 2 (0 to 7) | 4 (0 to 8) | Sin 15,000; Virtue 25,000; Angelic 5,000; Demonic 5,000; Generic 5,000 (the default for any ultimate without its own category) |
| **King** | 11 | 500,000 / 1,500,000 / 3,900,000 (9 stated) | 2M (Tantalus), 30M (Nyarlathotep) | 1,500 to 5,000 | 3 (0 to 4) | 3 (0 to 6) | 35,000 (`masteryKingUlt`) |
| **God** | 6 (3 with readable headers) | 2,501,000 (Yog-Sotohort) to 30,000,000 (Zehirete), or EP only (Tenebrosum) | 10M (Yog-Sotohort), 30M (Zehirete), 35M (Tenebrosum, plus 5 Uniques and True Demon Lord) | 5,000 | 3 (2 to 3) | 4 (3 to 7) | 50,000 (`masteryGodUlt`) |
| **Dragon** | 4 (2 with headers: Veldora, Velzard) | 1,000,000 | "500+ Bond Points" with the dragon, no EP | 5,000 | 1 | 2 | the five Lord-of-element dragon skills fall in the King mastery group (35,000) in the jar (Veldora, Velgrynd, Velzard, Velgaia, Velihility use `masteryKingUlt`) |

How it grows (READ numbers, my summary):

1. **Price grows with tier, not smoothly.** Unique 75K median, Lord 900K, King 1.5M, God from 2.5M MP up to 30M MP or tens of millions of EP. A Lord is already 12 times a Unique. The Dragon tier (1M MP) is cheaper than King; it is gated by a relationship (Bond Points), not by size.
2. **Ability count rises, then flattens.** Passives plus actives: Unique about 4, Lord about 7, King about 6, God about 7, Dragon 3. Class is not "more abilities"; God adds more actives (up to 7) and bigger numbers, Dragon has few abilities with large effects.
3. **Mastered versions are about x1.3 to x2 on the number shown.** Config pairs in the jar: Acnologia `magicDevouringMpDrain` 1.5 to 3.0, `gigaFlareDamage` 300 to 400; Lucifer `copyChance` 20 to 100; Raphael `analysisLevel` 10 to 30, `analysisRadius` 16 to 32, `copyChance` 50 to 90; Metatron `spGeneration` 5 to 10; Surya 10 to 15, `miracleBuff` 110 to 140; Hastur `movementSpeed` 0.02 to 0.04, `attackSpeed` 0.4 to 0.6; Susanoo barrier 4.0 to 6.0.
4. **Gates stack with tier.** Master the previous skill, enough MP, an EP floor, a kill or raid count, a task list (`Other`, `Other2`, `Other3` on the wiki). Jar examples: Abaddon 1,500 mob kills and 250 human kills; Sandalphon 15 raid wins and 25 mastered skills; Gabriel `gabrielHeroCount` 10; Nodens 150 mastered skills, 8 analysed ultimate sub-modes, 40M EP; Azathoth 100 named subordinates, 100 mastered skills, 40M EP (60M without a True Dragon) and an MP discount of 5,000,000 per True Dragon.
5. **The top is limited per world.** God class alone has an ownership cap (gamerule `trulygodclass`, `TrulyGodClassOwnership`, lang "This God-Class skill already has the maximum number of owners").
6. **Damage is per skill, not per tier.** The jar has no class multiplier. Values: `gigaFlareDamage` 300, True Dragon `breakDamage` 1,000 at `breakMagiculeCost` 1,500,000, element multipliers 1.5 to 2.5 (storm 1.5, scorch 1.5, earth 1.75, void 1.75, frost 2.5), `soulGluttonyDamage` 50, `purifyDamage` 40 (Metatron) / 50 (Surya). One "damage by class" number cannot be given; the table is what the mod defines per class.

### 8.2 Jar cross-check: acquire MP by mastery category (READ, `mpAcquirement`)

| Mastery category | Skills (n) | Acquire MP min / median / max | Wheel modes: median (range) |
|---|---|---|---|
| Generic (and the default for the 94 skills with no own category) | 10 (+94) | 120,000 / 525,000 / 5,000,000 (the 94: 250,000 / 900,000 / 5,000,000) | 5 (2 to 8); the 94: 5 (1 to 9) |
| Virtue | 2 (Raphael, Metatron) | 250,000 / 600,000 / 950,000 | 4.5 (3 to 6) |
| Angelic | 5 (Haniel, Raziel, Azrael, Sandalphon, Hamiel) | 500,000 / 900,000 / 1,050,000 | 4 |
| Demonic | 1 (Susanoo) | 600,000 | 2 |
| Sin | 16 (Lucifer, Satanael, Beelzebuth, Leviathan, Mammon, Asmodeus, Belphegor, Cthulhu, Gabriel, Samael, ...) | 750,000 / 1,500,000 / 3,000,000 | 5 (2 to 9) |
| King | 9 (Surya, Tantalus, Nyarlathotep, True Dragon, five Lord-of-element dragons) | 600,000 / 1,250,000 / 1,550,000 | 3 (1 to 6) |
| God | 4 (Nodens, Tenebrosum, Akashic Records, Azathoth) | 3,400,000 / 20,000,000 / 20,000,000 | 7 |

Wiki and jar agree on the shape: price about doubles from Lord to King and God is an order of magnitude above. They disagree on **mastery**: the wiki says "Points to Master 5,000" for every ultimate tier, the jar config says 5,000 to 50,000 per category. I cannot tell from the headers whether "Points to Master" is the same quantity as `getMaxMastery`; treat the jar as config truth and the wiki as the player-facing label.

### 8.3 Which Nightmares skills sit where

* **Lord (wiki)**: about 50 pages, e.g. Lucifer (1.7M MP), Raphael, Beelzebuth, Acnologia, Hestia, Gilgamesh, Galatine, Solomon (3.5M MP and 12M EP). In the jar: the Sin, Virtue, Angelic, Demonic and Generic categories.
* **King**: Abaddon, Cthugha, Cthulhu, Hamiel, Nyarlathotep, Shub-Niggurath, Surya, Tantalus, True Hero, Astaroth, Gilgamesh. The jar's King group is a different list (dragon-lord skills, Surya, Tantalus, Nyarlathotep). Abaddon is called King-class in its lang text yet has no mastery category in code (default group), so wiki tier and config category are maintained separately.
* **God**: Tenebrosum, Yog-Sotohort, Zehirete, Nodens, Azathoth, Akashic Records.
* **Dragon**: Veldora, Velzard, Velgrynd, Velgaia (wiki); in the jar the five Lord-of-element skills plus `TrueDragonSkill`.

### 8.4 Which of OUR ultimates sit in which class under that scaling

Wheel modes are from our source (READ). Class math is in `docs/ultimate_classes.md`.

| Our class | Our ultimates today | Our modes | Nightmares tier it matches | Fit |
|---|---|---|---|---|
| **LORD** (epCap x8, 2 %, mastery 10,000) | Adolla Burst (3), Black Wings, Hero's Argonaut (2), Immortal Will (2), Ki (10), Nen (17), Shishou (`ShishouModes.COUNT`), Lord of Gluttonous Greed (3); all 15 Dragon Ball and 27 Beast Will Ultimates (through `DbSkill`, `BeastSkill`) | 2 to 17 | Lord (median 4 actives, 2 passives, 900K MP, mastery 5,000) | Ki and Nen exceed any Nightmares Lord (max 8 actives); Argonaut and Immortal Will (2 modes) sit at the Unique end |
| **KING** (x10, 3 %, mastery 25,000, shred 0.70) | Gate of Babylon (10), Hell Conqueror, Lord of Summons (5), Goddess of Faith (5) | 5 to 10 | King (3 passives, 3 actives, 1.5M MP) | Our mastery 25,000 is below the jar's King default 35,000; our modes exceed the King median |
| **GOD** (x12, 5 %, mastery 50,000, kill 0.60, shred 0.80) | The Almighty, The Visionary (3), All Fiction (10 when rebuilt) | 3 to 10 | God (3 passives, 4 actives; 2.5M to 30M MP; 10M to 60M EP) | Mastery 50,000 equals the jar's God default. We have no EP gate, so a God skill has no entry price |
| **DRAGON** (x12, 4 %, mastery 15,000) | none | n/a | Dragon (1 passive, 2 actives, 1M MP, 500 Bond Points) | Empty. Nearest we own is Beast Will `bw_dragon` (kept LORD, see `docs/ultimate_classes.md`) |
| Not classed | Veldanava five: Sandalphon, Israfil, Jibril, Solomon, Hercules (10 modes each) | 10 | God by the owner's guide; Nightmares has its own Sandalphon (Angelic, 500K MP, 15 raid wins) and Solomon (Lord, 3.5M MP, 12M EP) | No class applied; 10 modes is above every Nightmares tier |

**Reading across: our ultimates have more modes than Nightmares', and no entry gates.** Nightmares keeps a tier in check by price and gates (EP floors, kills, mastery counts); ours does it by cost percent of max magicule. Both are valid; the cross-mod table in section 9 shows the other mods mostly follow Nightmares' shape.

**Targets for OUR numbers (a starting point for the owner, own units, nothing ported):**

| Our class | Wheel modes (actives plus toggles) | Passives | Mastery to full | Entry gate to add (proposal item 4) |
|---|---|---|---|---|
| LORD | 3 to 8 | 0 to 3 | keep 10,000 | mastered Unique (today); optional 500K EP |
| KING | 3 to 6 | 2 to 4 | 25,000 (35,000 only if the owner wants Nightmares parity) | 2M to 5M EP, or a count (kills, raids) |
| GOD | 4 to 7 | 2 to 3 | keep 50,000 | 10M+ EP, plus the single-owner cap (proposal item 7) |
| DRAGON | 2 actives | 1 | keep 15,000 | a relationship gate (Beast Will bond), no EP floor |

### 8.5 Wiki pages used (header fields and bullet counts only)

`unique__*` (71 pages); `lord__*` (50, including Lucifer, Alternative, Asmodeus, Michael, Sariel, Yog-Sothoth, Acnologia, Beelzebuth, Belphegor, Galatine, Hestia, Gilgamesh, Leviathan, Mammon, Raphael, Solomon, Uriel, Astarte, Azazel, Agni, Amatsumara, Artist, Beelzebub, King in Yellow, Necronomicon, Patience, Sunshine, Truth); `king__*` (Abaddon, Astaroth, Cthugha, Cthulhu, Gilgamesh, Hamiel, Nyarlathotep, Shub-Niggurath, Surya, Tantalus, True Hero); `god__*` (Akashic Records, Azathoth, Nodens, Tenebrosum, Yog-Sotohort, Zehirete); `dragon__*` (Veldora, Velzard, Velgaia, Velgrynd). Akashic Records, Azathoth, Nodens, Velgaia, Velgrynd and the king-tier Gilgamesh had no readable header, so the God and Dragon rows rest on 3 and 2 pages. Costs are what the page says and can be stale or mistyped (Hamiel shows "???k MP").

---

## 9. Cross-mod comparison for OUR scaling (Nightmares main, the rest as data points)

**Owner rule applied.** None of these mods is or may become a requirement of ours: no import, no `build.gradle` or `neoforge.mods.toml` change, no shared code or assets. Only base Tensura and ManasCore stay required. The numbers below only help choose OUR numbers.

"Ultimates / Uniques" counts are classes whose constructor names `SkillType.ULTIMATE` / `UNIQUE` (javap, INFERRED to be the skill type; helpers can slip in; config-typed skills read as `?`). Modes are `getModes()` literals (n = skills that had one); a skill whose count comes from a helper has no value.

| Mod | Ultimates | Modes per Ultimate: median (range), n | Uniques | Modes per Unique: median (range), n | Mastery / cost / gate signals (READ) |
|---|---|---|---|---|---|
| **Nightmares** (main) | 78 in the tag, 94+ classes | 5 (1 to 10), about 90 | 79 in the tag | wiki: 2.5 actives, 1 passive | max mastery by category 5,000 to 50,000; acquire MP 120K to 5M (God to 20M); EP gates 2M to 60M; God owner cap; `enableUltimateEvolution` per skill |
| **More Skills** | 20 | 5 (1 to 10), 15 | 21 | 2.5 (1 to 6), 18 | Unique max mastery constants 2,000 and 2,500; Ultimate mastery from config; mode names in lang |
| **Elite Tensura** | 7 | 5 (4 to 6), 6 | 7 | 2 (1 to 4), 6 | `ETExtraSkillConfig` and sibling configs; requirement and record system for unlocks |
| **Mysticism** | 1 | no constant | 31 | 3 (1 to 6), 28 | Unique max mastery 1,500; one config class per skill kind; its ultimates are said to be missing on 1.21.1 in a search snippet (not verified) |
| **Hardcore Evolutions** | 39 (all "God/Lord of X") | 2 (2 to 7), 25 | 0 | n/a | `EvolutionsConfig` plus 48 config classes; requirements patched into races (`_reqs`) |
| **Isekai Evolved** | 15 | 5 (3 to 7), 14 | 14 | 2 (2 to 7), 12 | max mastery constant 100 (a percent scale); hot-reload config; server mode policy |
| **Ascension** | 11 | 3 (3 to 4), 10 | 12 | 3 (1 to 4), 11 | max mastery 100 (Ultimate), 400 and 600 (Unique); gates: mastered Unique, True Demon Lord or True Hero, 5,000,000 Max EP, cooldown, per-ultimate tribute |
| **Minesura** | 1 | no constant | 15 | 2 (1 to 4), 14 | Ultimate max mastery 12,000; Unique 1,000 and 2,000 |
| **DxD** | 0 typed | n/a | 7 | 6 (3 to 7), 7 | many small Extra and Intrinsic skills (7 + 23); modes are heavy per Unique |
| **Origins** | 5 (registry `UltimateOriginsSkills`) | no constant | 0 | n/a | race-driven (Dragon Factors) |
| **Blade** | 1 | no constant | 1 | 3 (3), 1 | parent blessing skill with passive children |
| **More Skills Overlord, Leon boss, Restricted, Date a Slime** | 0 | n/a | 0 | n/a | intrinsic or boss content only (Overlord has 5 intrinsics) |

What the others add as targets for our numbers:

1. **Ultimate modes cluster at 5** (Nightmares 5, More Skills 5, Elite 5, Isekai 5; Ascension 3; Hardcore 2). Our outliers: Ki 10, Gate 10, Nen 17, Veldanava 10, All Fiction 10.
2. **Unique modes cluster at 2 to 3** (More Skills 2.5, Elite 2, Mysticism 3, Ascension 3, Isekai 2, Minesura 2, Nightmares wiki 2.5 actives).
3. **Mastery scales disagree wildly** (100, 400, 1,000, 2,500, 12,000, 50,000); only Nightmares has a per-class table. Our four-class table is already the cleaner design. The ratio to pick: God : King : Lord is about 5 : 3.5 : 1 in the jar (50,000 : 35,000 : 5,000 to 15,000); ours is 5 : 2.5 : 1 (50,000 : 25,000 : 10,000).
4. **Unique max mastery is 1,000 to 2,500** wherever it is set (Nightmares wiki 1,000; More Skills 2,000 and 2,500; Mysticism 1,500; Minesura 1,000 and 2,000); the Lord-to-Unique ratio is about 5 : 1 on the wiki.
5. **Only Ascension gates the Ultimate itself** hard (5,000,000 Max EP plus status); Nightmares gates per skill; the rest let mastery of the Unique be the gate, like us today.
6. **Numbers about double at mastery** (8.1 item 3). Our `Tier` has BASE / MASTERED / ULTIMATE / ULTIMATE_MASTERED, so the shape exists; the step sizes live in `skill/rules/*Rules` and were not re-read in this pass.

## 10. What we will not do (owner rule)

* No mod above becomes a dependency, optional or required. No import, jar, mixin target or lang key of theirs enters our source.
* Skill tags (proposal item 9) are our own files naming our ids; they reference no other mod, and stay an owner decision because they change how base Tensura reads our skills.
* Sections 8 and 9 are numbers to compare against, not code to port.

## 11. Could not be fetched, or not verified

* `tensura-reincarnated-nightmares.fandom.com`: does not resolve here (WebFetch `ENOTFOUND`). Replaced by the coordinator's local copy of the wiki.gg site.
* `trmysticism.wiki.gg`: does not resolve here; the Mysticism claim in section 9 is from a search snippet only.
* Japanese fallback (rule 10): one Japanese web search found nothing for this mod's Lord/King/God/Dragon classes (only series background on ciatr.jp and mod listings).
* Wiki pages with no readable header: Akashic Records, Azathoth, Nodens (God), Velgaia, Velgrynd (Dragon), Gilgamesh (King). God and Dragon rows are small samples.
* Not resolved: whether the wiki's "Points to Master" equals the jar's `getMaxMastery`.
* Not read: Nightmares' default essence costs and evolution condition bodies (section 6).
* Other mods' mastery and mode numbers cover only literal constants; helper-driven skills read as no value.
