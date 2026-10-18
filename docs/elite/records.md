# Records, Achievements & Titles (elite/records)

Part of the EliteTensuraAddon job (`docs/elite/plan.md`). The owner's inventory item: "Records, Achievements & Titles System. Records: record rarities, categories, and every unlock condition: Biome Discovery, Calamity Damage, Calamity Kill, Death Count, Distance Traveled, EP Reached, Forge Quality, Forge Count, FTB Chunks Claimed, Demon Lord status, Higher Being status, True Hero status, Kill Entity, Kill Count, Single Hit Damage, Skill Learned, Structure Discovery, Walpurgis Attended, War Kills, War Wins. Titles & Synergies: title rarities, requirements, attribute/effect/custom-stat bonuses, and the title synergy matching engine (`BuiltinSynergies`)."

**The original addon's source is not available.** Everything below is this mod's own design: the names, the numbers, the rarity ladder, the lists of records and titles, the synergy sets. The inventory fixed only the NAMES (the twenty condition types, `BuiltinSynergies`, "records / titles / synergies"); nothing of the original was copied or recalled as fact. Where the text says "designed here", that is what it means.

Nothing was run in Minecraft (the Gradle build cannot run in this session). What was only type-checked and what was run is listed at the end.

## What already existed (CLAUDE.md rule 5)

| Thing | Status | Why |
|---|---|---|
| Tensura's own titles / records / achievements | **EXISTS (nothing to duplicate)** | The Tensura Reincarnated API has no title or record system. It has *status flags* on the existence storage (`isTrueDemonLord`, `isDemonLordSeed`, `isTrueHero`, `isHeroEgg`, `isSpiritualForm`, `getEP`) and skills with a type. These are READ, never copied: the Demon Lord, True Hero and EP conditions are evaluated from Tensura's own values. |
| The mod's 452 advancements (`docs/advancements.md`) and the FTB quest book | **EXISTS, kept** | They are story and tutorial progress shown in the vanilla advancement screen and the quest book. Records are numeric, rarity-ranked, counted per player and carry bonuses. A different thing; both stay. |
| `core/discovery` (undiscovered things show as `???`) | **EXISTS, kept** | It hides items, pages, skills and creatures until found. It does not count anything or reward anything. Records borrow only the idea of a *hidden* flag (a hidden record is invisible until unlocked). |
| Magic Knight rank (`multiverse/MultiverseProfile`, `KnightRank`) | **EXISTS, kept** | A Black Clover social rank. Not related to titles. |
| FTB Chunks integration | **NEW (reflection bridge)** | The repo has no FTB Chunks code. It reaches FTB Teams by reflection (`quests/QuestTeams`); that lookup is reused to find the player's team, and `elite/records/FtbChunksBridge` adds the same style of soft bridge for FTB Chunks. |
| Records, record categories and rarities, the 20 conditions, toast, Records screen, `/records` | **NEW** | nothing like it existed |
| Titles (many held, one active, bonuses as transient attribute modifiers, effects, custom stats) | **NEW** | |
| Title synergies (`BuiltinSynergies` + data synergies) | **NEW** | |
| Menu button "Records" | **NEW, separate class** | Same pattern as the Skill Tree's `MenuInjection`, in `client/elite/records/RecordsMenuInjection`; no line of the existing injection changed. |

No part of this job is a REPLACEMENT: nothing was removed or changed in existing code except the one wiring line in `elite/EliteSystem.java`.

## How it works

```
data files (JSON)  ->  Catalog (immutable snapshot, swapped at /reload)
player events      ->  RecordData (attachment nusmp:elite_records: counters, bests, found sets, unlocked, titles)
                       + Live (EP, statuses, FTB claims, vanilla stats, hooks, skills)   ->  FactsBuilder  ->  Facts
Facts + Catalog    ->  Engine.run: unlock records, grant titles (repeat until stable)
active title + held titles -> Bonuses -> TitleApplier: transient attribute modifiers, short effects
```

Everything under `elite/records/rule` is pure Java (Gson only, no Minecraft class) and is unit-tested; the rest is thin glue.

### Records

A record is `{id, name, description, category, rarity, hidden, condition}`. Rarity ladder (designed here): common, uncommon, rare, epic, legendary, mythic, worth 1, 2, 4, 8, 16, 32 points. Categories: exploration, combat, survival, power, skills, crafting, dominion, war, calamity. A record unlocks once, for good: EP, claims, statuses, distances and deaths are kept as the highest value ever seen, skills as the set ever learned, so losing EP or forgetting a skill never takes a record away.

The twenty condition types (`rule/ConditionEvaluator`), each a pure function of a `Facts` snapshot:

| Type | Parameters | Source of the number |
|---|---|---|
| `biome_discovery` | `biome` (id, `*` wildcard) or `count` [+ `namespace`] | the biome the player stands in, sampled every beat |
| `structure_discovery` | `structure` (id, `*` wildcard) or `count` | `StructureManager.getAllStructuresAt` every 4th beat (config `trackStructures`) |
| `calamity_damage` | `amount` | counter, fed by `RecordEvents.calamityDamage` |
| `calamity_kill` | `count` [+ `calamity` id] | counter, fed by `RecordEvents.calamityKill` |
| `death_count` | `count` | max(vanilla statistic, own counter) |
| `distance_traveled` | `meters` [+ `mode`: all walk sprint swim fly elytra boat mount] | vanilla statistics (cm to m) |
| `ep_reached` | `ep` | Tensura `IExistence.getEP`; UNAVAILABLE without Tensura's storage |
| `forge_quality` | `quality` 1..6 or common..mythical | max(own counter fed by `RecordEvents.forgeCrafted`, `EliteHooks.forgeBestQuality`) |
| `forge_count` | `count` [+ `min_quality`] | max(own counter, `EliteHooks.forgeCraftedCount`); `min_quality` uses the own per-quality counts |
| `ftb_chunks_claimed` | `count` | FTB Chunks via reflection; UNAVAILABLE when the mod is absent or its API differs |
| `demon_lord_status` | `status`: `seed` or `true` | Tensura `isDemonLordSeed` / `isTrueDemonLord` |
| `higher_being_status` | `status`: `spiritual` or `divine` | designed here: `isSpiritualForm` = spiritual; a race whose path starts with `divine` or `god` = divine |
| `true_hero_status` | `status`: `egg` or `true` | Tensura `isHeroEgg` / `isTrueHero` |
| `kill_entity` | `entity` or `entities` (ids, `*` wildcard such as `tensura:*`), `count` | own per-entity kill counts |
| `kill_count` | `count` [+ `group`: all hostile passive player boss] | own counters |
| `single_hit_damage` | `damage` | best single hit seen in `LivingDamageEvent.Post` |
| `skill_learned` | `skill` id, or `count` [+ `skill_type`: resistance intrinsic common extra unique ultimate] | ManasCore `SkillAPI` (Tensura `Skill.getType`); the set ever learned |
| `walpurgis_attended` | `count` | counter, fed by `RecordEvents.walpurgisAttended` |
| `war_kills` | `count` | counter, fed by `RecordEvents.warKill` |
| `war_wins` | `count` | counter, fed by `RecordEvents.warWin` |

A type whose source is missing answers UNAVAILABLE (never zero): it never unlocks, and the screen says "Unavailable on this server" instead of a bar stuck at nothing. Shipped: **210 records** (`data/nusmp/elite/records/<category>.json`), every one of the 20 types used at least twice, all six rarities, all nine categories, 5 hidden ones.

### Titles

`{id, name, description, rarity, tags, requirements, bonuses}`. Requirements (all must hold) are `record`, `ep`, `condition` (any of the twenty), `title`, `records` (N unlocked, optionally of a category and a minimum rarity), `points`, or `any` (at least one of a list). A title is earned automatically when its requirements hold, is kept for good, and a player holds many. The player picks ONE active title (in the Records screen: click the row, or `/records title <id>`).

Bonuses: attribute modifiers (`attribute`, `amount`, `operation` add_value / add_multiplied_base / add_multiplied_total), effects (`effect`, `amplifier`) and custom stats (a closed list, `rule/StatNames`). Shipped: **81 titles**, all six rarities.

How bonuses reach the player (`TitleApplier`): on the beat, and at once when the active title is changed, the desired set is computed (active title + every matched synergy, times config `bonusScale`) and applied as TRANSIENT attribute modifiers (never saved, so they cannot get stuck in a player file) with a stable id per attribute and operation (`nusmp:elite_title/<attribute>_<operation>`). A modifier that is no longer wanted is removed; effects are short ambient effects renewed each beat (and removed when no longer wanted); custom stats are not applied to anything but kept as totals.

Custom stats: `xp_bonus_percent`, `kill_heal` and `boss_damage_percent` are implemented by this system (experience drop, heal on kill, damage to `c:bosses`). `forge_quality_bonus`, `coin_bonus_percent`, `ep_gain_percent`, `war_damage_percent`, `calamity_damage_percent` and `council_vote_weight` are numbers other subsystems read with `RecordEvents.customStat(uuid, name)`; until they do, a title that grants one is a flavour line. This is stated in the title text only through the bonus line, and listed here so nobody assumes otherwise.

Balance (designed here, adjustable with `bonusScale`): bonuses are small on purpose (+0.5..+6 flat, +2%..+15% multiplicative), because they stack with an active title and several synergies. A data test caps the sizes.

### Synergies

A synergy applies while the HELD titles (not only the worn one) contain all its `requires` titles AND, if it names a `tag`, at least `count` held titles carry that tag (`*` = any title; `rarity:<id>` is an implicit tag of every title). `rule/SynergyMatcher` is the pure matcher (given the held set and a title-to-tags function); `rule/BuiltinSynergies` holds 18 built in (14 by named titles, 4 by tag counts), `data/nusmp/elite/titles/synergies/*.json` adds 8 (a data synergy with a built-in's id replaces it, so a pack can retune one). Total shipped: **26**.

### Toast, screen, key, button

* Toast: a small overlay in the rarity colour (`RecordToastOverlay`, a GUI layer like the Arts bar); several unlocks queue. Config `toasts`.
* Records screen (`RecordsScreen`): mouse-driven, a tab per category (with unlocked/total), Titles, Synergies, a filter (all / unlocked / locked), rarity colours, progress bars with numbers, scroll wheel or scroll-bar drag, hover for the full text, click a held title to make it active (click again to remove), the active title and the bonuses in effect in the footer. Reached by the key (default I, rebindable; `key.nusmp.records_open`) and by a "Records" button injected into the Codex (and the two older Four Kingdoms / Status screens) by `RecordsMenuInjection`.
* Lang: `tools/elite/records_lang.json` (generated by `tools/elite/gen_records_lang.py`; the game falls back to the English text declared in `RecordsText`). `en_us.json` is not edited.

### Public API for the other subsystems

`com.newuniverse.nusmp.elite.records.RecordEvents` (static, null-safe, never throws, no-op when the system is off, every method also has a `UUID` form that acts on an online player):

```
walpurgisAttended(player)        warKill(player)          warWin(player)
forgeCrafted(player, quality)    calamityDamage(player, amount)
calamityKill(player)             calamityKill(player, calamityId)
grantRecord(player, recordId)    customStat(uuid | player, statName)
```

`EliteHooks` (owned here, set at init): `recordUnlocked(uuid, recordId)`, `hasTitle(uuid, titleId)`, `activeTitle(uuid)`, `heldTitleCount(uuid)`. They answer for online players from the attachment, and for players who logged out since the server started from a snapshot taken at logout; for a player the server has not seen since it started they answer false / "" / 0 (their data is not loaded).

### Config (`nusmp-elite-records-server.toml`, section `[eliteRecords]`)

`enabled` (master switch), `toasts`, `announceRare` (chat line for legendary and mythic records), `titleBonuses`, `synergies`, `bonusScale` (0..5), `beatTicks` (10..400), `trackStructures`.

### Commands

`/records` (summary), `/records list <category>`, `/records titles`, `/records title <id>|clear`; operators: `/records grant|revoke <player> <record>`, `grant_title|revoke_title <player> <title>`, `reset <player>`, `check <player>`, `problems` (the validation report of the data files).

### Data a pack can add or override

`data/<ns>/elite/records/*.json`, `elite/titles/*.json`, `elite/titles/synergies/*.json`: one object, an array, or `{"records": [...]}` / `{"titles": [...]}` / `{"synergies": [...]}`. A later pack replaces an entry with the same id. A broken entry is skipped with a line in the log and in `/records problems`; the rest still loads. `tools/elite/gen_records_data.py` generates the shipped files (the single source of the 210 records, 81 titles and the data synergies).

## Tests

* Pure JUnit (`src/test/java/.../elite/records`, 61 cases): every one of the 20 evaluators (threshold, one below, progress fraction, wildcard ids, unavailable), bad parameters rejected, `RecordData` round trip and hostile input (wrong types, NaN, negative, absurdly long lists, active title not held), bests only rise, the engine chain (record, title, title needing a title, record-count and points requirements, never takes anything away), the synergy matcher (named, tag, `*`, rarity tag, mixed, empty, progress, combined bonuses, data replacing built-in), the cross-reference validation, the shipped data (counts, all 20 types used, all rarities and categories, no problems, every record and title reachable by some facts, bonus sizes), the lang file against `RecordsText`, and the `Describe` lines.
* In-game self-tests (`/multiverse mega selftest elite.records.data|nbt|chain`, run by the CI server smoke): `data` checks that every attribute, effect, entity, biome, structure and skill the data names exists in the real registries; `nbt` round-trips and feeds hostile tags; `chain` plays warKill -> record -> title -> active title -> modifier on the real attribute and off, a synergy on and off, a custom stat, the four hooks and the API's null safety, on a fake player.

## Unverified

* Nothing was run in the game. The Java was type-checked against the API stubs (the only errors are the stub artefacts "KeyMapping / MobEffectInstance is abstract", with a negative control showing real errors are reported). The pure rules were run (JUnit shim).
* `FtbChunksBridge` reflects `FTBChunksAPI.api().isManagerLoaded()/getManager()`, `ClaimedChunkManager.getOrCreateData(Team)` and `ChunkTeamData.getClaimedChunks()`; those names follow FTB Chunks 21.1 from memory and could not be run. A mismatch shows as "unavailable" with one warning in the log, not as a crash.
* Tensura attribute ids (`tensura:max_magicule`, `max_aura`, `magicule_regeneration_multiplier`, `ability_mastery_gain`, `ability_learning_gain`) follow the field names of `TensuraAttributes` (one is confirmed in the repo); the self-test `elite.records.data` lists any id that does not resolve. Skill ids `tensura:great_sage`, `demon_lord_haki`, `hero_haki` are checked by the same self-test.
* The "higher being" reading (spiritual form / race path `divine*` or `god*`) is this mod's interpretation of a Tensura term.
* Screen layout and the toast animation are untested on a real display (the layout is plain integer arithmetic; text is trimmed to fit).
* Nothing outside this system calls `RecordEvents` yet (the war, council, forge and calamity builders do that); the counters those conditions read stay at zero until they do, and `forge_*` also read `EliteHooks` so the forge can instead just publish its numbers.
