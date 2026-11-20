# Boss scaling after the Leon study: Black Clover x3 counts (fix 1) and every boss scales by party size (fix 5)

Source: `docs/boss_reference_leon.md` sections 3.3 (G10, G11) and 4 (fixes 1 and 5), approved by the owner. Both fixes are server rules, on by default, each behind a SERVER key that restores today's exact behaviour.

## What it is, how it is read, what is not included

**Fix 1, the x3 counts.** The per-second damage cap of a boss (`BossBudget`, `dpsCapFraction` 6% of max health) used to take its share of the full max health. The Black Clover x3 raised the max health, so it raised the cap by x3 too, and the shortest possible kill stayed about 17 s whatever the bar. Now the cap is a share of the BASE max health (max health divided by the Black Clover modifier, `BossStatBuff.healthFactor`), so a x3 boss keeps the cap of its unbuffed bar and the shortest kill is about 50 s. Floor scale and party scale still raise the cap (they are not divided out). The 50% one-hit cap on x3 damage is unchanged.

**Fix 5, party size for every boss.** `ArenaScaling` (until now Lucius only) is now applied to every boss of the Zogratis framework (the Dark Triad, Goliath, Charybdis spawn, Devil Avatar, God of Order, every ArcBoss: early, Midnight Sun, Sins, Spade, DanMachi, Goku, Sinbad, Zagred and so on). The fighters present when the fight starts (players of the fight, alive, survival/adventure, within 100 blocks of the arena) are counted once, in `ZogratisBoss.startFight`:

| fighters | health | damage |
|---|---|---|
| 1 | x0.70 | x0.80 |
| 2 | x1.00 | x1.00 |
| 3 | x1.15 | x1.06 |
| 4+ | x1.30 | x1.12 |

Health is one permanent multiplied-total modifier (`anime_multiverse:boss_stat/party_health`); the bar keeps its share (a full boss stays full). Damage is a factor inside `bossDamage` and `strike`, so every melee hit and every move that goes through them is scaled. An abandoned fight (everyone left) takes the modifier off, and the next fight reads its own party.

**One combined cap.** The party factor is cut back (never below 1) so that the whole stack stays under a stated maximum: health (Black Clover x3 times floor scale, which is at most x2.5, times party) at most **x8**, damage (x3 times floor damage, at most x1.6, times party) at most **x5**. Worked cases: x3 on the deepest floor and 4 players: 7.5 x 1.0667 = 8.0 health (not 9.75); x3 alone with 4 players: 3 x 1.30 = 3.9. A small party (x0.70, x0.80) is never touched by the cap.

**Not included:** Lucius (his own `LuciusGod` scaling, read each stage, is unchanged; `partyScaled()` is false for him so he is never double-scaled), the Tier List rows (`TierBoss`, data rows), the Labyrinth Guardians and the 200 Decennial bosses, the Floor Warden and ZagredBossEntity (they are not Zogratis-framework bosses). Spell damage that a boss applies through its own `hurt` call instead of `bossDamage`/`strike` is not scaled (see "not checked").

## Settings (SERVER)

| key | file | default | off means |
|---|---|---|---|
| `bosses.dpsCapUsesBaseHealth` | `nusmp-boss-budget-server.toml` | true | the cap follows the full max health (today's behaviour) |
| `bossRules.partyScaling` | `nusmp-boss-rules-server.toml` | true | no party scaling (today's numbers; an existing modifier is cleared at the next fight start) |

## NEW / REPLACED / KEPT

NEW: `BudgetPolicy.capBasis`, `BossBudgetConfig.dpsCapUsesBaseHealth`, `BossStatBuff.healthFactor` and `setFactor`, `ArenaScaling.healthCombined/damageCombined` and the maxima, `BossRulesConfig.partyScaling`, `ZogratisBoss.applyPartyScale/clearPartyScale/partyScaled`, self-test `boss.leon1`, tests `BaseHealthCapTest`, `PartyCapTest`, `LeonFixesSourceScanTest`.
REPLACED: nothing is removed. The cap of a Black Clover boss changes by design (fix 1) and bosses other than Lucius now carry a party factor (fix 5); both are switchable.
KEPT: every `ArenaScaling` method Lucius uses, Lucius's own scaling, the Black Clover x3 modifier and its 50% one-hit cap, floor scaling, the Tier List and Decennial bosses, all boss tables.

## Worst-case cost (rule 11)

* Fix 1: no new work. The cap was already recomputed on the boss tick; it now also does one attribute-modifier map lookup. No packets, no entities.
* Fix 5: once per fight start per boss: one pass over the `fighters` set (at most the online players, each one `getPlayerByUUID`), two modifier writes and one health write. Damage: one multiply per hit. No per-tick work, no scans, no packets, no spawns. Cleared on abandon; the modifier is saved with the boss like the x3.
* Server only, no client class touched, per-boss state only (no static "current player").

## Tests

Pure: `BaseHealthCapTest` (basis, setting off equals old arithmetic, kill time 17 s to 50 s, minimum), `PartyCapTest` (table, small party never raised, whole stack never above x8 / x5, x3 times floor case, bad input, Lucius's old methods), source scan `LeonFixesSourceScanTest` (read once at fight start, Lucius and Tier List opt out, defaults, no tick). Self-test `boss.leon1` (CI server): a Black Clover boss's live cap equals the base-health arithmetic; a non-Black-Clover boss is scaled to 1 and to 4 fighters, keeps a full bar, and is put back.

## Not checked

* Nothing was played. The self-test and the real compile are CI's job; this session compiled the changed classes with javac against the source layout only.
* Which moves reach a player through `bossDamage`/`strike` and which call `hurt` directly was not audited boss by boss; a move that does the latter is not party-scaled (health is).
* The fight start counts `fighters`, which fills from players within 40 blocks; a squad that arrives after the first hit is not counted until the next fight.
* Exact feel of the x3 fights (50 s minimum) and of solo x0.70 on 30 bosses was not tuned in play.
* `smoke.yml` still has to get the keys `boss.leon1` and `boss.leon3` (not edited here, as instructed).
