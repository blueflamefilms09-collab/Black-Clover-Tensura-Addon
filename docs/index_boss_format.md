# How Tensura No Index builds its bosses (study for Goku, Kakashi and Sinbad)

Source: the jar `tensura-no-index` 1.3.7 (a Toaru Majutsu no Index addon for Tensura), read with `javap` (class names, fields, constants). Nothing was copied; this is a description of the shape, so our own bosses can follow the same discipline.

## 1. Folder layout: one package per boss, one shared package for the rules

`bosses/` holds 20 boss packages (arthur, brunhild, gabriel, highpriest, jailbreaker, kazakiri, marian, metatron, misakaworst, nephthys, niang, othinus, rightseat, rimuru, titan, touma, velgaia, versailles, mini, stage) and one shared layer of 26 classes at the root.

Every boss package has the same parts (Arthur as the example):

| Part | What it does |
|---|---|
| `ArthurEntity` (+ `Live` inner) | the body: attributes, goals, state |
| `ArthurFight` | ONE fight object per arena slot: the challenger, the fighters, the boss bar (`ServerBossEvent`), the phase beat, the damage window, abandon / orphan sweep, logout handling |
| `ArthurArena` | where the fight happens (an arena the fight owns; blocks cannot be broken or placed in it) |
| `ArthurRules` | pure rules (strike shapes, numbers), testable without the game |
| `ArthurSummon` / `ArthurSeat` | how the fight is started and who may |
| `ArthurRewards` | what the winner gets: a named item, a favour flag, XP (10000), a true-name finisher |
| `ArthurItems` | the signature item (a crown) with a trinket definition |
| `ArthurFx` / `ArthurPose` / `client/*` | effects, model pose, boss bar, renderer layer (client only) |

The lesson: a boss is a **fight object plus a body**, not only an entity. The fight owns the bar, the arena, the participants and the cleanup; the entity only fights.

## 2. The shared layer (`bosses/*`): rules every boss obeys

* `BossTiers.Tier` — 18 named tiers, each with `rank`, an EP `pool`, and two "heavy" fractions (`targetHeavy`, `nativeHeavy`). Pools run from 10,000,000 (dragon Touma) down through 8M (Arthur), 5M (Othinus, Marian), 4M (Touma), 2.4M (Metatron), 2M (Nephthys, priests), 1.6M, 1.4M, 800k, 700k, 600k. A boss has a tier, and its HP, EP and damage derive from the tier, so bosses are comparable.
* `BossHealthCap`, `BossCaps` — one lifted ceiling for max health, so a huge boss does not overflow.
* `BossGuard` — a **damage budget per second** per boss (`budgetPerSecond`, `floorPerSecond`, `limitLoss`, `clampLoss`): a boss cannot lose more than its budget in a window, so a single huge hit or a command cannot erase a long fight. It also names the "killing effects", haki effects and `/kill` as administrative paths that are watched.
* `BossPercentRules` — percent-of-max-health damage is capped; `minFightSeconds(tier)` is the shortest fight a tier allows; `antiHealTicks`/`antiHealKeep` stop a boss being healed back to full by tricks.
* `BossEpRules` — EP pool, number of "bodies", **shares by damage dealt** (`MIN_SHARE`), `payout` to every contributor.
* `BossSummary` — a damage **ledger** per player; on death it prints a summary, and each player's share of the damage decides a **rarity of their reward**: COMMON (any), UNCOMMON (5% share), RARE (10%), EPIC (20%), LEGENDARY (35%), each with a stack bonus (0 / 10% / 25% / 50% / 100%). Loot is given directly to each fighter, not dropped.
* `BossImmunity`, `BossWard`, `BossWardFx`, `ShadowImmunity` — typed immunities and wards.
* `BossAggro`, `BossRetaliation`, `BossAirGuard` — aggro rules, retaliation, and an anti-flying-cheese guard.
* `BossVoice`, `BossSound`, `BossSfx` — lines and sounds on a shared channel.
* `BossPool`, `RaidBossEntity` — a pool of fights and the raid base entity.
* `BossAftermath`, `BossDamageShare` — cleanup after a fight and the damage-share bookkeeping.

## 3. What this means for our three bosses

Our Goku, Kakashi and Sinbad already have: health scaling by player count, EP scaling, forms/phases, a boss bar, hit caps. What the Index format adds, and what is not in them yet:

1. **A tier with a pool** (health, EP, damage derived from one number) — we have per-boss config only.
2. **A damage budget per second and a minimum fight length** — we have a hit cap per blow only.
3. **Anti-heal** and a **percent-damage cap** — not present.
4. **A damage ledger with a share-based reward rarity** (Common to Legendary) and direct loot to each fighter — our drops come from loot tables to the killer.
5. **A fight object** that owns the bar, arena, participants and cleanup — our bosses keep this in the entity and events.
6. **Rewards that belong to the boss** (a named item, a favour, XP) — ours are generic cores and armour.

## 4. Plan to bring Goku, Kakashi and Sinbad onto it (next releases)

* 4.104.0 (this release): this study, plus the rarity, Shishou and telekinesis fixes.
* 4.105.0: a shared `BossFrame` of pure rules (tier, damage budget per second, minimum fight length, anti-heal, share ledger with rarity) in the existing `perf`/`arc` style, behind `boss.indexRules` (off restores today's fights); Sinbad is the first user (phases, more health, Magi layer).
* 4.106.0: Kakashi on the same frame (more health, visible phases, Naruto layer).
* 4.107.0: Goku on the same frame (Ki layer visuals, anime drops: Saiyan armour with heavy debuffs).
* Then: boss-key and teleport items (4.104 plan), and Luffy and Deku as new bosses on the finished frame.

## 5. Not done and not claimed

* The jar was read as bytecode only; no behaviour was run, so the numbers above are constants read from the classes, not observed in a fight.
* Which tier maps to which of our bosses is a decision for the owner (suggestion: Goku above Sinbad above Kakashi).
