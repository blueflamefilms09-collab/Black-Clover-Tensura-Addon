# Evolution skills: the server engine (3.26.0)

What this is: a skill on a line (`nusmp_lines`) can carry an `evolution` block. The player earns it by playing (kills, damage, survival ...). When the counter reaches the target the skill is **evolved**: its passives use the `evolved` values and its **Ultimate** art unlocks (once the skill is also mastered). A new pair of kinds, `debt_store` (passive) and `debt_burst` (active), makes the Debt mechanic.

All rules are decided on the server. The client only draws text from data it already receives.

## Route data (what the engine reads)

- Route: `evolution { requirement, count, text, ultimate_name, params? }` (`data/RouteEvolution`). `requirement` is one of `kills, elite_kills, boss_kills, damage_dealt, damage_taken, lethal_survived, arts_cast, ally_buffs`. `count` is 1..`EVOLUTION_MAX_COUNT`. `params.damage_kind` is allowed only on `damage_taken`.
- Node: `ultimate: true` on exactly one active art (not a stance, not a passive). `evolved { ...params }` on a passive replaces its numbers once the skill is evolved (`data/EvolvedParams`).
- A route with an `ultimate` art but no `evolution` loads with a warning and the art is never locked. A classic route (no block) is untouched.
- Wire: the evolution block is appended to the route wire and to `LineCard` (old readers of the same version are not supported, client and server must match).

## Counters (`line/Evolution`, pure rules in `line/EvolutionRules`)

Stored per player in persistent data `nusmp_line` under `evo.<skillId>` (int, clamped 0..max). Only counted while the skill is **owned and on**, the skill has an evolution, and it is not evolved yet (an evolved skill stops counting).

| requirement | counted when | hook |
|---|---|---|
| kills | the player kills a foe (not self, not a player, not allied, not a tamed mob) | `LivingDeathEvent` |
| elite_kills | the kill was an elite (boss tag or max health >= 100) | `LivingDeathEvent` |
| boss_kills | the kill was a boss | `LivingDeathEvent` |
| damage_dealt | whole points of damage dealt to a hostile foe (fraction is carried) | `LivingDamageEvent.Post` |
| damage_taken | whole points taken, optionally only of `damage_kind` | `LivingDamageEvent.Post` |
| lethal_survived | a hit left the player at <= 2 health and alive; at most one per 5 s | `LivingDamageEvent.Post` |
| arts_cast | a line art of the skill is cast (the Ultimate itself is not counted) | `LineCast` |
| ally_buffs | allies other than the caster reached by `buff_aura` / `heal_pulse` | `ActiveExecutors` |

Debt damage is dispatched with the `busy` flag, so it never feeds counters or passives.

## Evolved, the gate and the card

- Evolved = counter >= count (`Evolution.isEvolved`). On the moment it flips, the player gets an action-bar line (`msg.nusmp.evolution.evolved`).
- `PassiveCtx` reads `node.evolved()` for numbers and raises the level to the node max when evolved. `LinePassives.active` works this out once per route per beat.
- Ultimate gate (`LineCast`): before the cooldown check, `Evolution.gate` answers OPEN, NOT_EVOLVED or NOT_MASTERED (`MasteryLevels.mastered`). Anything else than OPEN returns `Result.LOCKED` and a red action-bar line says why. With `evolutionSkills = false` the gate is always OPEN.
- Card: the skill description gets the requirement line and the "Evolves into" line from the route (the client holds a node-less stub route built from `LineCard`, which carries the evolution block). Progress "n / count", "needs mastery" and the locked state of the Ultimate come from the **skill instance tag** (`nusmp_evo`, `nusmp_evo_n`, `nusmp_evo_i`), which ManasCore already syncs to the owner. No new packet was added. The mirror is rate limited to once per 2 s per skill and only written when the value changed.

## Debt (`line/DebtLedger` pure, `line/Debts` glue)

- `debt_store` (passive): when the owner damages a foe, `store_pct` of the (post-law) damage is stored as Debt on that foe, capped at `max_pct` of its max health. Every `tick_seconds` the foe takes a small hit from the stored Debt (a quarter of it, at most 5 percent of its max health), with the owner as the damage source. Entries expire after `hold_seconds`. `vfx` names the shape drawn.
- `debt_burst` (active): hits up to `targets` foes (never more than 16) within `radius`, preferring foes that carry Debt. Each takes `damage` (through `BalanceLaw`) plus `mult` x the Debt, the extra capped at the foe's max health (25 percent for bosses). A player target is held to `BalanceLaw.cap`. A Debt is consumed only if the blow landed.
- Memory only: `UUID owner -> UUID foe -> Entry`. At most **20 foes per owner**. Pruned on: hold time, foe death, owner death, owner logout, owner dimension change, server stop.
- Both honour `evolutionSkills = false` (Debt off).

## Config

`nusmp-evolution-server.toml` (SERVER): `evolutionSkills` (default true). False: counters stop, the Ultimate needs no evolution, `debt_store` and `debt_burst` do nothing.

## Language

`tools/lang/evolution.json` is a flat fragment (7 keys) for the lead to merge into `en_us.json`. The code has an English fallback table (`EvolutionText`), so a missing entry never shows a raw key. `EvolutionTextTest` keeps the two equal.

## Cleanup

Logout: Debt of the owner dropped. Death and dimension change: Debt of the owner dropped. `PlayerEvent.Clone`: the `evo*` keys are copied to the new player if missing (safeguard in case persistent data is not copied on respawn). Server stopping: the whole ledger is cleared.

## Worst-case cost (rule 11)

- **Entities spawned: 0.** Nothing is created; Debt hits are `hurt` calls on existing foes.
- **Per tick: 0** extra work. There is no tick listener. Everything runs on events and on the existing 10-tick line beat (staggered per player).
- **Counter hooks:** one read of the player's owned-line list and a few int operations per event, only for lines that have an evolution and are not evolved; no world scan, no entity query. `Evolution.mirror` writes at most one tag per skill per 2 s.
- **Debt tick, per player per 10-tick beat:** at most 4 hurt calls and 2 effect shapes, found by entity-by-UUID lookup in the loaded level (no chunk loading, no radius scan). A foe that is gone or beyond 48 blocks is dropped at once. Max ledger size: 20 foes x online players.
- **Debt burst:** one cast = one `nearest` query with a fixed radius and at most 16 hits; cooldown from the node.
- **Packets:** no new packet type. Effect shapes go through `VfxSpawn.send` (nearby players only, `PerfBudgets.effectPacket`), at most 2 per player per beat = 4 per second per player at the very worst, plus the burst effect. Progress reaches the owner only through the instance-tag sync, at most once per 2 s per evolving skill, only when changed.
- **Client draw:** no new rendering. Card text only.
- **Profiling:** `PerfProbe` sections `s.line.evolution` and `s.line.debt`.

## Tests

- Pure: `EvolutionRulesTest`, `DebtLedgerTest`, `EvolutionTextTest`, `EvolutionSchemaTest` (route parsing, wire, card round trips, refusal cases), `LineKindsTest` (the two new kinds and the ultimate/evolved fixture, line 4), `LineSchemaTest`.
- Self-test (CI server smoke, prefix `evolution.`): `evolution.counters` and `evolution.debt` (`line/EvolutionSelfTests`). Add both to the smoke list.

## Not covered

Seen in a real game: nothing yet (no Gradle or game run in the build session). Not included: new art, new VFX textures, balancing of any shipped line (the line rows are written by another builder; the self-test drives the real hooks on the first shipped line that carries an evolution, and says so if none does).
