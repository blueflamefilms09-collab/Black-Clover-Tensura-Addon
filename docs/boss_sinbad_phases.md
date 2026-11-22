# Sinbad: phases, more health, the Magi look, anime drops (4.105.0)

Studied first: `docs/index_boss_format.md` (how Tensura No Index builds bosses). Sinbad is the first boss on that discipline; the shared `BossFrame` (tier pool, damage ledger with rarity payout) is NOT built in this release (see "Not included").

## NEW / REPLACED / KEPT
| | What |
|---|---|
| NEW | Three phases: **King of Sindria** (power 20-40, as before), **Djinn Master** (from power 60: a break with rays and feathers, a second health bar through the framework's `refillForMode("magi_phase_2")`, equips 16 s instead of 20 s, the direct switch may be chosen in the last 6 s instead of 3 s), **Seven Seas** (the Gates, as before, 12 s equips, its own refill). |
| NEW | Health: his 3200 is multiplied by `sinbad_boss.healthFactor` (2.5, range 1-20) while `sinbad_boss.phases` is on. Written once as a permanent modifier, the share of health is kept, never a heal. |
| NEW | `SinbadFx`: the Magi layer in his fight (`sinbad_boss.magiVfx`): Djinn Equip forging, a Magoi aura while a djinn is worn, a Rukh flock along Bararaq Saiqa and every Seven Seas step, the Extreme Magic over Bararaq Inqerad-Saiqa and the last Seven Seas step, the phase break. |
| NEW | Player Djinn: the Full Djinn Equip and the start of an Extreme Magic also send the Magi layer (`djinnLook.magiVfx`, default on). |
| NEW | Pure test `SinbadPhaseTest`, self-test `sinbad.boss.phases`. |
| REPLACED | Sinbad's drop table `boss_signature/sinbad_boss.json`: boon water breathing, conduit power, luck II, strength II; extras magic stones, gold blocks (Sindria's trade), emerald blocks, amethyst shards (seven vessels), heart of the sea (Seven Seas), netherite scrap. The one-of-one stays. The old table is kept at `docs/classic_drops/sinbad_boss.4.104.json`. |
| KEPT | The old magic circle (shown with `magiVfx` false), every move, the move order, the equip rule, Magoi Manipulation, foresight, all saves (new tags are read with defaults), the Time Magic VFX. |

## Worst case (rule 11)
* Entities: 0 new. Packets: an aura packet every 25 ticks while a djinn is worn (about 2.4 per second per boss), one equip packet per switch, one Rukh/Extreme packet per ranged move, 8 for the Seven Seas finale; all to nearby players through the effect budget. At most 4 Sinbads (`globalCap`).
* Client: Magi quads per effect are capped by the layer (24-70, engine cap 100).
* Server: one health write per boss; nothing is scanned per tick.

## Not included / not verified
* Not seen in a game. Numbers (2.5x health, 16 s equips) are first guesses for you to judge.
* The Index-style frame (tier pool, damage budget per second, minimum fight length, anti-heal, damage ledger with rarity) is planned for the boss after this one so it is built once for all three; Sinbad's own per-second damage budget is still the arc framework's.
* Phase 2 changes timing, not a new move set.

## Lag and bug audit of Sinbad, Goku, Kakashi and the boss framework (read-only review, code reading only, nothing run in a game)
Fixed here: queued `later` steps firing after a fight reset (shared `ZogratisBoss`, epoch counter).
Found and NOT fixed in this release (for the Goku and Kakashi releases, each needs its own look): the particle helpers in `arc/Tells` are not routed through the shared particle budget; `foes(32)` has no cap on its result list in a crowded arena; Goku and Kakashi `bossTick` have no try/catch (Sinbad's has); Goku's `CENTRES` map is only cleaned on use; Goku can still punch during his farewell; a Kakashi dash flag can stay set when a cast is interrupted; block and clip reads near an unloaded chunk could load it (unconfirmed); per-viewer particle fan-out is not counted by the budget.
Checked and refuted: a Kakashi gap in the realm door gate (an unlisted boss is available by default).
Not read: Goku/Kakashi config and math files, the boss commands, `SdsaStrikes` beyond line 200.
