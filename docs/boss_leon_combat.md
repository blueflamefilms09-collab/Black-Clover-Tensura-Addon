# Boss combat from the Leon study: reactive guard (#4) and attack timeline (#2)

Built from `docs/boss_reference_leon.md` (section 1.2 for Leon's numbers, 3.1 gaps G2 and G3, section 4 rows 2 and 4). Nothing here was seen in a game: see "Not checked".

## What is built and how it is read

### Reactive guard (fix #4): `entity/zogratis/ReactiveGuard.java` (pure)

Six bosses guard: **Meliodas, Ban, Heath Grice, Vetto, Mars, Udaeus**. (Escanor has his own counter and is left out. The list is `ReactiveGuard.OPT_IN`, keyed by character id; `ArcBoss.reactiveGuard()` reads it. Any boss class can also override `ZogratisBoss.reactiveGuard()`.)

How a fight reads: when a player or a player's summon is within 5 blocks (leaves the stance beyond 6), the boss may react to the first hit it takes:

| Reaction | Default | What the player sees |
|---|---|---|
| Dodge | 30% per hit, 8 invulnerable ticks, then 60 ticks cooldown (never more than one a second) | The hit does nothing; sweep sound and a few crit sparks. Every hit in the next 8 ticks does nothing too. |
| Block | 70% per hit, 12-tick block action, then 40 ticks cooldown, lets 25% of the hit through | Shield sound and sparks; every hit in the next 12 ticks is cut the same way. When the 12 ticks are over the boss answers once with a counter swing at the attacker (plain melee damage, through the normal damage law). |

Dodge is rolled first, then block, as Leon rolls each separately. The boss does **not** guard while it is exposed (after an ultimate), winding up a move, in recovery, transforming, or refilling at the Gates, and it ignores nothing else: true-damage and magic-tagged sources (`BYPASSES_ARMOR`), the void and `/kill` are untouched.

**Where it sits.** In `ZogratisBoss.hurt`, after `filterDamage` and `mitigate` (so Meliodas's Full Counter, Mars's frontal armour and the other bespoke lines have already run) and **before** `PowerCurve.clampDamage` and the per-second budget (`dpsBudget.hurt`). It only returns a smaller number. It never heals, never writes health, never touches the budget or the regeneration guard.

**Proof it cannot make a boss unkillable** (also pinned in `ReactiveGuardTest`):
1. A reaction needs its cooldown to be over, and the constructor clamps every cooldown to at least twice its action window (block 40 >= 2 x 12, dodge 60 >= 2 x 8, dodge also >= 20/cap). So a block covers at most 12/40 = 30% of the time and a dodge window at most 8/60 = 13.3%.
2. A block lets 25% through (clamped to at least 5%), a dodge lets 0. A foe that hits every single tick therefore gets at least `1 - 0.30 x 0.75 - 0.133 = 64%` of its damage past the guard, in the long run, whatever the chances are (even at the 90% clamp of the keys). A test runs ten minutes of a hit on every tick at the worst odds and checks `through / offered > 0.55`.
3. The guard sits in front of, not instead of, the framework's lines: the power-step clamp still stops one hit skipping a step, the DPS budget (6% of max health a second) still caps what lands, and the budget only limits from above, so the minimum kill time of `docs/boss_dps_budget.md` is unchanged and the guard can only make a fight longer than that floor by at most the 1/0.64 factor above.
4. No regeneration is added or allowed (`guardHurt` contains no heal or `setHealth`; a source scan test pins it).
5. Chances are clamped to 0.9, so a boss is never certain to avoid a hit.

### Attack timeline (fix #2): `entity/zogratis/AttackTimeline.java` (pure, client safe)

A row per melee move: clip ticks, hit tick, optional second hit tick, damage multiplier per hit. A boss with rows cycles through them in order (a rotation, so the moves can be learned). The swing starts on the server (`swing()`), the damage lands on the **hit tick** (and a second hit on its own tick) if the target is still within 1 block of reach, instead of the instant melee on a bare cooldown. A boss that starts a move, is exposed or loses its target drops the swing in the air (a stalled swing never lands a stale hit: it is forgotten after its clip length).

| Boss | Rows (clip / hit / second / multiplier per hit) |
|---|---|
| Meliodas | slash_horizontal 28/12/-/1.4, slash_rising 30/13/-/1.5, cross_combo 38/12/22/0.95 |
| Ban | jab 26/11/-/1.3, slash_rising 30/13/-/1.5, cross_combo 36/11/21/0.9 |
| Escanor | cleave 34/16/-/1.7, slash_horizontal 30/13/-/1.5, cleave_heavy 38/17/-/1.9 |
| Heath Grice | slash_horizontal 28/12/-/1.4, slash_rising 30/13/-/1.5 |
| Vetto | claw 24/10/-/1.2, slash_advancing 28/12/-/1.4, cross_combo 34/10/20/0.85 |
| Mars | slash_horizontal 30/13/-/1.5, slash_advancing 32/14/-/1.6 |
| Udaeus | slam 36/17/-/1.8, cleave_heavy 40/19/-/2.0 |
| Minotaur | axe_chop 36/17/-/1.8, cleave_heavy 42/20/-/2.1 |
| Black Goliath | fist_slam 40/19/-/2.0, cleave_heavy 44/22/-/2.2 |

The clip/hit pairs of the sword bosses are Leon's own six clips (attack 30/13, horizontal 28/12, rising 30/13, advancing 31/13 (rounded to 32/14 for Mars), heavy cleave 38/17, cross combo 38/12/22); the heavy monsters use longer ones in the same shape. The multiplier of each row is its clip length over 20 ticks (the old fixed cooldown), halved per hit for a two-hit move, so **damage per second is unchanged** (`AttackTimeline.neutral`, pinned by a test and by the self-test): only the rhythm and the telegraph change. The time to the next swing is `max(16, clip x power's cooldown multiplier)`, so a stronger power still swings faster, as before.

All damage still goes through `ZogratisBoss.strike` (power multiplier, per-boss scale, victim EP factor, balance law cap), then the boss's own damage budget logic as before.

**How the animation agrees (client).** The pose clock `client/zogratis/ZogratisPose` used to start every swing 0.2 s into the shared 0.9 s attack clip. With a row it plays the arm raise of the clip (0 to 0.2 s) stretched over the lead-in so it ends exactly on the hit tick, then the clip runs on at its own speed. The client learns which row from one synced int (`SWING_MOVE`, the row's index; -1 = no timeline) and reads the same pure table through `ZogratisBoss.swingRow()` in `ZogratisRenderer.frame`; no client class is reachable from the table (rule 9). The second hit of a cross combo has no pose of its own (it needs the new clips of fix #3, not built here) and is a sound-less second hit on the same clip.

### Settings (SERVER config `nusmp-boss-rules-server.toml`, group `[bossCombat]`)

| Key | Default | Range | Meaning |
|---|---|---|---|
| `reactiveGuard` | true | | false = the six bosses take every hit as before (the guard is not even built) |
| `guardBlockChance` | 0.70 | 0.0 to 0.9 | chance a hit starts a block when the block cooldown is over |
| `guardDodgeChance` | 0.30 | 0.0 to 0.9 | chance a hit starts a dodge when the dodge cooldown is over |
| `attackTimeline` | true | | false = the old instant melee on the fixed cooldown and the old 0.2 s client lead (the server then syncs -1) |

Both read at every use, so a change applies to a boss that is already standing.

## NEW / REPLACED / KEPT (CLAUDE.md rule 5)

* **NEW:** `ReactiveGuard`, `AttackTimeline` (+ its `Runner`), the four keys, the `SWING_MOVE` synced int, `ZogratisBoss.reactiveGuard()/timelineKey()/swingRow()` and the guard/counter/timed-swing methods, `ArcBoss` overrides, `Frame.attackHit` in `ZogratisPose`, self-tests `boss.leon2.tables` and `boss.leon2.live`, tests `ReactiveGuardTest`, `AttackTimelineTest`, `ZogratisPoseTimelineTest`, `Leon2SourceScanTest`.
* **REPLACED (for the nine timeline bosses, behind `attackTimeline`):** the instant melee on a bare cooldown is replaced by the swing that lands on its hit tick; the client's fixed 0.2 s lead is replaced by the row's hit tick. Nothing was deleted: both old paths stay in the code and run when the key is false, for every boss without a row, and for every other boss.
* **KEPT:** the whole power curve, the DPS budget, `BalanceLaw`, the no-regen rules and the Gates refill; every move, wind-up, cooldown and table; Escanor's counter (he takes the timeline but not the guard); Meliodas's Full Counter and Mars's Nemean Armor (the guard stacks on them, after them); all clips, models and sounds (no asset was made: fix #3 is not part of this); the Time Magic VFX untouched.

## Cost (rule 11): worst case per boss

* **Server, per tick, per boss:** the timeline adds two integer compares and (only when the boss is in melee reach and its melee is ready) one config read; the guard adds nothing per tick except one `takeCounter` compare. No world scan, no chunk load, no spawn, no allocation per tick.
* **Per hit taken:** O(1): one distance, two random doubles, one small record for the settings compare (a few bytes), no entity query. The counter looks one entity up by id.
* **Per second, bounded by the cooldowns:** at most one guard action every 40 ticks (block) and one every 60 ticks (dodge): at most 1 sound packet and 1 particle packet (4 crit particles) per 8 ticks, sent to players near the boss only, so at most about 5 small packets a second per boss in the extreme, and normally under 1. At most one counter swing per 40 ticks.
* **Per swing:** one `swing()` animation packet (as before) plus at most one synced-data packet for `SWING_MOVE` when the row changes, so at most 1 extra small packet per swing (a swing is 24 to 44 ticks), to tracking players only.
* **How many bosses:** one boss per arena (the 96-block present check); each boss's state is its own fields (no static, no "current player"), keyed to nobody, dropped with the entity.
* **Client:** per drawn boss per frame one `instanceof`, one table lookup, no allocation (the rows are static records); far bosses are culled as before.

## Tests and self-test

Pure: `ReactiveGuardTest` (defaults, clamped odds, stance, i-frames, cooldowns, per-second dodge cap, counter once, never more than offered, never past the budget, bounded guarded share for every odds value), `AttackTimelineTest` (hit-tick math, second hit, stale swing dropped, neutral damage, rotation index, clip mapping), `ZogratisPoseTimelineTest`, `Leon2SourceScanTest` (pure classes, the guard sits before the clamp and the budget, server keys with ranges, no scan, wired). Self-tests (server smoke): `boss.leon2.tables` (ids are roster bosses, every swing neutral, the guard keeps at least half of a foe's damage) and `boss.leon2.live` (a Meliodas is summoned, draws no swing before it fights, takes a foe-less hit, is removed).

**The lead has to add `boss.leon2` to `SMOKE_EXPECT` in `.github/workflows/smoke.yml`** (this change did not touch the workflow, as instructed).

## Not checked

* Nothing was seen in a game: not the feel of the block and dodge odds, not whether the stretched arm raise looks right on each model (the shared clip's raise was measured on `arc_escanor` only: blow starts about 0.2 s in), not the Minotaur's Blender body (it follows the clip clock, so it should agree), not the sounds.
* The code was not compiled here (the Gradle cache has no Minecraft jars; the pure classes and tests were compiled and run with `javac` and the JUnit console jar, 4 existing source-scan tests too). CI is the first real compile of the `ZogratisBoss`, `ArcBoss`, `ZogratisRenderer` and `Leon2SelfTests` edits.
* The synced `SWING_MOVE` and the swing animation packet are two packets; if the client draws the first frame of a swing before the data arrives it uses the old lead for that one swing.
* The second hit of a cross combo has no new pose (clips are fix #3).
* Interplay with bespoke counters beyond reading the code (Meliodas Full Counter, Mars frontal armour, Ban's immortality rule) was not exercised.
* No lang keys were added (no fragment needed).
