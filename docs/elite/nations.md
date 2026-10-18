# Nations & War (elite/nations)

Part of the EliteTensuraAddon job (docs/elite/plan.md). The owner's inventory line for this area:

> Nations & War System. Nation Management: creation (founding), leveling, metrics, role permissions (custom roles), sync mechanisms. Economy & Treasury: ledger systems (Treasury Ledger), UBS banking integration
> (UBS Cash & Interest hooks), perks (Nation Perks), treasury bridges. Contracts & Missions: contract types, bounty posting (Contract Bounties), mission tiers, reward structures. Reputation & Spawners: reputation
> tiers, nation spawner trackers, entity tags, tier scaling. Nation War & Alliances: war phases, alliance proposals, player bounties, war score handling (War Score Handler), war spoils, war history tracking, tick handlers.

**Honesty first.** The original addon's source is not available, only these names. Every number, rule, file format and class below is designed here, from the names. Where a rule is a guess it says so
("design decision"). Nothing was run in a game: the Gradle build cannot run in the build session. What is verified and how is in the last section.

## NEW / EXISTS / REPLACE (CLAUDE.md rule 5)

| Part | What the mod already had | Verdict |
|---|---|---|
| Nations (found, members, leader, capital, level, metrics) | Nothing. Tensura Reincarnated ships no nation, settlement or faction classes (searched the API dumps and the ID lists). `multiverse/Squads` and the dungeon `PartyService` are small groups, not political entities. | **NEW** |
| Custom roles and permissions | FTB Teams has its own roles inside a party; the squads use Captain / Vice / Member | **NEW**, separate from FTB (a nation is bigger and persistent) |
| Treasury ledger, banking bridge, interest | The Black Market sells for Tensura coins; the coin economy (`EliteHooks.wallet`) is another builder's job | **NEW**; uses `EliteHooks.wallet` read-only |
| Contracts, bounties, board | Dungeon floor quests (System, 1.55.0), FTB quests pack (`quests/`) | **NEW**, different system (nation-wide, repeatable, data-driven) |
| Reputation tiers | Social class, Magic Knight stars (`multiverse/`), Avatar aptitudes | **NEW** (per player per nation) |
| Guard spawner tracker | Boss budget, encounter director (dungeon only) | **NEW** |
| Wars, alliances, war score, spoils, history | Nothing | **NEW** |
| PvP / griefing rules | `DungeonGuard` (unbreakable dungeon), PartyGuard | **NEW** rules, **never applied inside the dungeon dimensions** (they keep their own guard) |

Nothing existing was removed, replaced or edited except the single wiring line in `elite/EliteSystem.java` (nations section). No version, CHANGELOG, README, LIBRARIES, gradle.properties,
neoforge.mods.toml or en_us.json edit; the lang fragment is `tools/elite/nations_lang.json`.

### How nations coexist with squads and parties

* **Squads (Magic Knight) and FTB parties** stay what they are: a few players working together, with their own commands. A nation is the persistent political layer on top: a squad can sit inside a nation, two
  squads of one nation can be at war with a third nation. Nations never read or write FTB data and `Squads`/`PartyService` are untouched.
* **Dungeon parties** (`labyrinth/mega/party`) decide whose dungeon floors you walk. Nations do not: war score, PvP rules, capital protection and guards are all switched off inside the dungeon dimensions
  (`DungeonGuard.on(level)`), so a war can never reach into a party's instanced floors. (`warScoreInDungeon` can turn the scoring on.)

## What was built (package `com.newuniverse.nusmp.elite.nations`, client in `client/elite/nations`)

```
core/      Permission, Role, Roles, Nation, NationLevels, Perk, PerkTable, GuardRules, Res, NationText (generated)   pure
econ/      Ledger, InterestPolicy, TreasuryBridge, UbsBridge (reflection)                                            pure except UbsBridge
contract/  ContractType, ContractTemplate(s), Contract, ContractRules, Reward, Bounty                              pure
rep/       ReputationTier, ReputationBook                                                                          pure
war/       War, WarPhase, WarRules, WarBook, AllianceBook, PvpRules                                                pure
event/     14 NeoForge game-bus events (plain values)
net/       SnapshotPayload, RequestPayload, ActionPayload, NationSnapshot, NationNet
world/     NationsConfig, NationsData (SavedData) + NationNbt, NationService, Treasury, ContractService, WarService,
           ReputationService, GuardService, NationPerks, NationActions, NationEvents, NationTicker, NationCommand,
           NationBlocks + NationBannerBlock + ContractBoardBlock, NationsLoader (reload listener), Msg, NationSelfTests
NationApi, NationsRegistry
```

The rules are pure classes with no Minecraft types (JUnit-tested); the services wrap them with the server, the clock and the config. Everything is server-authoritative.

### 1. Nation core
* **Founding**: `/nation found <name>` or the screen. Price by config `founding.costMode`: `coins` (the wallet hook), `item`, `both`, `free`. **Until the coin economy is merged the wallet refuses everything, so a
  server without it must use `item` or `free`** (default is `coins`, as the final mod will have the Hub). The founder becomes leader and gets a **Nation Banner** item.
* **Capital** = the block `nusmp:nation_banner`, placed by the leader (one per nation; refused in the dungeon dimensions, for non-leaders, and closer than 2 x `capitalRadius` to another capital). Only the leader
  (or a creative operator) can break it, which clears the capital. Right click opens the screen. A second block `nusmp:contract_board` opens the screen on the contracts tab.
* **Members**: invite (expires), accept, decline, leave, kick, transfer leadership. Member cap = `founding.baseMemberCap` + perk `member_cap`, ceiling `maxMemberCap`.
* **Roles**: four defaults (leader rank 100, officer 60, member 30, recruit 10) plus up to `maxCustomRoles` custom ones. Permissions: invite, kick, treasury, war, contracts, build, diplomacy, roles. Rules
  (tested): you act only on ranks strictly below yours; you hand out only permissions you hold; the leader role is fixed; a custom role is created just under its creator.
* **Level**: `score = members x 20 + min(treasury / 1000, 300) + contracts x 10 + areaScore x 1 + warWins x 50 + nationXp` (all weights configurable). Level n needs `base x (n-1)^growth` (150, 1.6 by default),
  max level 10. The level may fall if the metrics fall; `NationLevelUpEvent` only fires past the highest level ever announced (so deposit-withdraw cannot farm it). **Area score** (design decision) = +1 per
  `areaIntervalSeconds` while at least one member stands inside the capital area, capped.
* **Perks** per level from `data/nusmp/elite/nations/perks.json` (a datapack can replace it; the built-in table is identical and a test keeps them equal): treasury cap, member cap, guard count, shop
  discount, contract bonus, board slots, and **member attribute buffs near the capital** (transient modifiers, removed when you leave the area or the nation).
* **Sync**: the server sends one compact `SnapshotPayload` (your nation in detail, a one-line directory of others, bounties, your reputation) only while your screen is open, when something concerning you changed,
  at most every `syncMinInterval` ticks. The client sends one `ActionPayload(action, a, b, n)` per button; the server revalidates it with the same code the commands use (`NationActions`) and rate-limits (10 / second).
* **Screen**: key `N` (rebindable) or banner / board right click. Five mouse-driven tabs: overview, members and roles, treasury, contracts and bounties, diplomacy. Drawn by hand from the snapshot (no widgets to
  rebuild, so typing is never lost). The art of the two blocks is first-pass (`tools/elite/gen_nations_assets.py`).

### 2. Economy
* **Ledger** (pure, tested): every deposit, withdrawal, transfer, interest, tax, reward, spoils, fee and admin move with who / when / why / amount / balance after; capped history (`ledgerMaxEntries`, 200); the
  balance never goes negative or above the cap (`baseTreasuryCap` + perk).
* **Treasury bridge**: `NationApi.treasuryBridge()` returns a `TreasuryBridge` (balance, deposit, withdraw, cap) for other systems to pay in.
* **Interest**: `InterestPolicy` hook. Modes `builtin` (default: 0.5 percent per hour, minimum balance 1000, at most 5000 per period), `ubs`, `off`; `NationApi.setInterestPolicy` replaces it at run time.
* **UBS bridge** (`econ/UbsBridge`, optional): looks the mod up by id (`ubsModIds`), and only if you also name its API class and static methods in the config does it use the player's UBS cash for deposits and
  withdrawals and an optional interest method. **The UBS API could not be inspected; the method names are config, not assumptions, and nothing in the bridge is verified against a real UBS jar.** Without UBS
  (or without the config) it answers "not available" and the treasury uses the wallet hook and the built-in interest.
* **Tax** (optional, leader / treasury permission): a percent of every member's contract coins goes to the treasury instead (`maxTaxPercent`).

### 3. Contracts and missions
* Types (all checkable from a server event): `kill_mob` (id or `#tag`), `mine_block`, `craft_item`, `explore_biome` (distinct biomes, optionally within a tag), `deliver_item` (hand in on the board) and bounties
  on players or mob types. No escort or build contracts on purpose (not checkable).
* **58 templates** in `data/nusmp/elite/nations/contracts/*.json` (vanilla ids and tags only; table in `tools/elite/gen_nation_contracts.py`). Five **tiers** (Bronze to Mythic): a nation level unlocks tier
  1 + (level-1)/2; a player's reputation tier caps the tier they may take (neutral 2, friendly 3, honored 4, revered 5; unfriendly 1; hostile and hated none). Rewards: copper coins (wallet), nation XP,
  reputation; the nation's `contract_bonus` perk adds a percent; system rewards grow 2 percent per level above the template's minimum.
* The board refills every `boardRefreshMinutes` and immediately after a completion; a member with the contracts permission takes up to `maxTakenPerPlayer`; progress comes from game events (kill, block break,
  craft, a biome check every 5 seconds). Known limit: mining counts blocks that were placed and re-broken.
* **Bounties**: coins held in escrow on a player or a mob type, posted by a player (wallet, plus a 10 percent burned fee) or by a nation (treasury). The killer collects; poster and (non-war) fellow nation
  members cannot claim; lapsed bounties refund. Max 5 per player.

### 4. Reputation and spawners
* **Reputation** per player per nation id (any faction id works), -1000 to +1000, seven tiers: hated, hostile, unfriendly, neutral, friendly, honored, revered. Effects: shop price percent (160 down to 80,
  `NationApi.priceModifierPercent`), contract tier cap, board access, and whether the nation's guards treat you as an enemy (hostile and hated). Gains: contracts, bounties, war victory. Losses: killing a
  member outside a war, killing a guard. Decays toward zero (1 point per hour).
* **Guard tracker**: a nation with a capital keeps `base + perLevel x (level-1) + perk` guards (2, 0.75, cap 12 per nation, 80 on the server), at most 2 per 15-second round, only while a player is within 96
  blocks (no chunk loading). The mobs come from the **entity-type tags** `nusmp:nation_guard_tier_1..5` (and `nusmp:nation_guard`, the union), so a datapack decides the roster: shipped vanilla only
  (iron golem, vindicator, pillager, evoker, ravager); add Tensura or any modded mob by adding its id with `"required": false`. Tier = 1 + (level-1)/2; higher tiers get +25 percent health, +1 damage and +1 armour
  per tier. Each guard carries a marker in its persistent data (nation, tier) so the tracker re-adopts survivors after a restart and discards the orphans of a disbanded nation. Their target goals are replaced:
  they strike back, fight hostile monsters, and attack only enemies of their nation (at war, or hated), never members or allies.

### 5. War and alliances
* **Phases** (pure state machine, tested): DECLARED (60 s notice) -> PREPARATION (10 min) -> ACTIVE (60 min, or first to `targetScore` 200) -> CEASEFIRE (30 min truce window) -> ENDED (archived into the capped
  **history**; the pair cannot re-declare for `truceMinutes`). A ceasefire is proposed and accepted by the other side; the leader can surrender; the attacker can withdraw before the fighting; an operator can end it.
  Declaring needs the war permission, level >= 2, no alliance, no open war or truce with that nation, a declare cooldown, and fewer than 3 open wars.
* **War score handler**: +10 per kill of an enemy-nation member (the same victim scores again only after 10 minutes: anti-farming), and **siege**: every 10 seconds, the side that outnumbers the other inside the
  enemy capital area scores +1 and after 6 held intervals captures it for +25 (objective). Optional automatic **war bounty** on a member who killed 3 enemies (`warBountyCoins`, off by default).
* **Spoils** of a victory (tested to conserve coins): 10 percent of the loser's treasury (cap 50 000) goes to the winner, +100 nation XP, +25 reputation for each winner member with their nation, and a 20-minute
  Strength + Resistance buff for the winner's online members. Draw / ceasefire: none.
* **Alliances** (pure, tested): propose / accept / decline / break; proposals expire (10 min); a counter-proposal accepts at once; none with yourself, a nation at war with you, or one at war with an ally; at most
  3 allies; 60-minute cooldown after a break; war on an ally is refused.
* **Protection**: no friendly fire inside a nation or between allies; between different nations PvP is allowed **only while a war between them is ACTIVE** (`warOnlyPvp`); players without a nation are untouched
  (vanilla settings). Capital areas: outsiders and members without the build permission cannot break or place blocks and explosions spare the blocks (`protectCapital`, `protectExplosions`). War never allows
  griefing. All of it is off inside the dungeon dimensions.
* **Tick handler**: one server-tick listener that returns at once except every `tickInterval` ticks, then each job runs on its own interval (war phases, alliance expiry, siege, area score, boards and bounty
  expiry, interest, reputation decay, guards, perk modifiers, screen sync). Each job is wrapped so one failure never stops the others.

## Commands
`/nation gui|found|disband confirm|info|list|level|perks|invite|accept|decline|leave|kick|transfer|rename|color|tax|capital [item]`, `role list|set|create|delete|perm`, `treasury balance|ledger|deposit|withdraw|tax`
(deposit and withdraw take an optional `ubs`), `contract list|take|abandon|deliver`, `bounty list|post player|post mob|cancel`, `rep`, `war declare|status|history|ceasefire|decline|surrender|cancel`,
`ally list|propose|accept|decline|break`, and for operators `admin disband|xp|deposit|endwar|rep|selftest`. Every action command is the same code path as the screen's button.

## Config (`nusmp-elite-nations-server.toml`)
Sections `general` (master `enabled`, `tickInterval`, `syncMinInterval`, `maxNations`), `founding`, `levels`, `economy` (including the `ubs*` keys), `contracts`, `reputation`, `guards`, `war` (`enabled`,
all phase lengths, scores, spoils, alliance limits, protection switches, `scoreInDungeon`), `perks`. Every number is a key; defaults are above.

## API for the next builders
`NationApi` (static, null-safe, plain types) and the 14 events; signatures are listed in the builder report and in the class Javadoc. `EliteHooks.nationOf` and `EliteHooks.atWar` are set by
`NationsRegistry.init` (`atWar` = declared, preparing or fighting; `NationApi.isFighting` = ACTIVE only). A ceasefire window is neither.

## Tests and what is only type-checked
* **JUnit (pure, run here)**: `LedgerTest`, `RolesTest`, `LevelPerkTest` (levels, perks, guard scaling, reputation), `WarTest` (state machine, score, siege, spoils, history, alliances, PvP verdicts),
  `ContractTest` (matching, progress, delivery, tiers, board picks, tax split, template parsing, bounties), `LangFragmentTest` (every message key has a text; the lang fragment equals the fallback map),
  `ShippedDataTest` (all 58 contracts and perks.json parse; built-in perks equal the file; tags and block assets exist).
* **In-game self-tests** (`elite.nations.*`, run by the CI smoke test or `/nation admin selftest`; NOT run here): data, found, roles, treasury, contracts, war, alliance, guards, persistence (save, load, save is
  identical), api. They use two `FakePlayer`s and clean up after themselves.
* **Only type-checked** (against the API stubs, with a negative control; the only errors left are the stub artefacts "X is abstract; cannot be instantiated"): all Minecraft-facing code, the screen, the
  payloads, the block classes. Never run in a game: the screen layout, the key, block placement and breaking events, explosions, guard AI after `removeAllGoals`, the attribute perks, the real wallet / UBS
  interplay, datapack tag loading, the contract JSON with real vanilla tags (an unknown tag simply never matches).
