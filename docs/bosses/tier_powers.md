# Tier boss powers and how they are balanced (3.39.0)

Status: **built in 3.39.0**. This file is the design AND the reference: every number below is a constant in `TierTrait`, a row in `TierRosterF` / `TierRosterD`, or a value the tests hold (`TierPowersDocTest` fails when this file and the code drift apart). It answers: "give the bosses their power / abilities from their anime, and tell me what power and how to balance them".

Scope: the **16 light tier bosses** of 3.33.0 (six F, ten D). Light Yagami already is his anime power (the Death Note scene) and is not touched; he is a gag boss and is left out of the balance bands. The Zogratis bosses, the arcs' bosses and the SDS / Black Clover characters are not touched.

Sources (rule 10): the English and Japanese wikis could **not** be fetched from the build environment, so every power below is from memory and tagged **named** (the series' own name), **theme** (something the character really does, shaped and named by me) or **filler** (invented so the fight has enough to do). Nothing is wiki-verified; correct a character in one place (its roster row) once the hosts are allowed.

## 1. What a "power" is here

Before 3.39.0 every one of the 16 had five moves cut from nine generic shapes (cleave, slam, wave, bolt, volley, marked, rush, beam, swarm). That is why Foxy, Ao and Code felt like the same boss. A power adds two things on top, both small and bounded:

1. **A signature trait** (rule 2 of `RULES.md`): one passive or phase rule from the character's anime, with a telegraph and a counter the player can learn. A boss has one or two.
2. **Moves re-cut to the anime**: same five slots (four ordinary moves and one ultimate, or the shapes the row had), but the numbers, statuses and names follow the character. Three new statuses were appended to `TierMove.Effect`: **POISON** (poison that never kills: stops at 1 health), **SHIELD_BREAK** (a raised shield is disabled for 5 s) and **STRIP** (removes beneficial effects: one per hit, all of them when it comes from an ultimate or a crowd move). Pull is a trait, not a status.

Not in this release: the **1-of-1 player skill** each boss is meant to drop (rule 1 of `RULES.md`, claim registry, +10% skill gain). That is a separate, larger release (decision 1 at the end).

## 2. The trait catalogue (all server side, all capped)

Every trait is data (a `TierTraitSpec` on the row) run by one class, `TierTraits`, from the hooks the boss framework already has (`bossTick`, `mitigate`, `damageScale`, `hurt`, `strike`, the move tell and the move landing). None spawns an entity, changes a block or heals. A trait looks at the same at-most-24 foes a move does, at most once per second. Six traits are **bound to one move of the boss** (marked "move" below): the row names which move triggers or carries them.

| Trait | Price | What it does (as built) | The counter |
|---|---|---|---|
| SLOW_FIELD | 2 | Players within 6 blocks (9 at the Gates) get Slowness I, refreshed every second; the ring is drawn | stay outside the ring and use range |
| STEAM | 2 | A player who hits him in melee within 4.5 blocks catches fire for 3 s (once per 2 s per player) | range, water, fire resistance |
| ARMOUR (Hierro) | 3 | Melee, non-magic damage is cut by 35%; the cut is **off** while he is exposed after his ultimate | magic and ranged damage, the open window after the ultimate |
| BALANCE (move) | 3 | While the bound move winds up (its telegraph) he weighs the damage dealt to him; when it lands, whoever dealt more than half of it is weakened 6 s, and a lone fighter also takes 40% of what they dealt back (at most one hit of his attack) | share the damage, or pause while he raises the scales |
| SOUL_MARK | 3 | Each of his hits on a player adds a mark (max 3, 15 s each); at 3 marks the player is weakened 5 s and he hits 10% harder for 8 s, and the marks are cleared | break contact, let the marks run out |
| MIRROR (move) | 2 | For 6 s after the bound move he reflects 25% of the damage he takes (at most 6 health per hit, never lethal, capped by the balance law's per-hit cap) | stop hitting for the 6 s, or use ranged chip |
| SYSTEM_LOCK | 3 | His hits strip one beneficial effect (at most one per victim every 2 s) | re-apply after the hit |
| PULL (move) | 2 | The player marked by the bound move is dragged toward him in 4 small impulses (0.4 blocks each, 8 ticks apart, about 1.2 s), starting after the move's wind-up | break line of sight, knockback resistance |
| SPORE_POISON | 2 | His hits poison for 3 s (never lethal: stops at 1 health) | milk, regeneration |
| FUSION | 2 | At 66% and 33% health: +15% damage and +10% speed for good (no size change); announced in chat | burst him down before the fusion |
| SIGHT | 2 | Invisible or sneaking players within 14 blocks get the Glowing outline for 2 s | break line of sight with blocks |
| KARMA (move) | 2 | The bound move marks its target for 15 s: his hits do 20% more to them | milk clears the mark |
| ECHO (move) | 2 | The bound move repeats once after 1.5 s: the same telegraph again, no bonus damage | keep moving after the first one |
| UNTOUCHABLE (move) | 2 | 35% less damage for 4 s after the bound move (his guards arrive) | dodge the guards, hit after the window |
| CLIMB | 2 | Each power step of the fight (40, 60, 80%): +8% speed and +8% damage, at most 3 steps | kill pace: he is slowest at the start |
| SHIELD_BREAK | 1 | All his melee hits disable a raised shield for 5 s | dodge instead of blocking |

Budget (points a boss may spend on traits): **F 5, D 7** (+2 per letter later: C 9, B 11, A 13). One boss carries one or two traits, never more than its budget (the unit test refuses a row that does). Echo, mirror and untouchable are **not allowed on an ultimate** (an ultimate already leaves him exposed for 3 s, a defensive stance on top would make it a wall); Balance may sit on the ultimate. The same trait may appear on bosses of the same tier, the numbers are the same.

## 3. How they are balanced (the method)

Seven layers, from the one the owner already tuned to the new knobs:

1. **Tier stats** (existing, `TierLetter`): F 700 health / 10 attack, D 1000 / 13; a row scales them a little.
2. **Pressure index (new, measured):** the expected damage a boss deals per second. Computed by the pure class `TierBalance.pressure` (checked by `TierBalanceTest`) and by its deterministic Python twin `tools/tier_bosses/tier_pressure.py` (run it to retune a row without compiling; the two print the same table). It simulates the real scheduler for 180 s: one move at a time, the ultimate first when it is ready, otherwise the highest weight times time-since-use, a 50% hit rate (60% for the ultimate), the ultimate leaving him exposed. The traits that deal damage are in the number: Climb, Fusion, the soul-mark frenzy (30% uptime), Karma (40% uptime), a move that Echo repeats counts twice, steam (0.4 per second), poison (0.3 per second). **Rule: inside a tier every boss lies within ±25% of the tier's mean at 60% power**, checked by `TierBalance.bandProblems`.
3. **Hit size caps:** an ordinary move deals at most **1.6x the boss's attack**, an ultimate at most **2.0x** (checked by the test; the largest built are 1.3x and 1.5x, so a retune has room but never spikes).
4. **Trait budget:** the points above (F 5, D 7). A boss with a 3-point trait has fewer points left for a second one.
5. **A counter for every trait, and a window:** every trait has a stated counter and a stated window in which it is off or weak (`TierTrait.counter()` and `window()`, refused blank by the test).
6. **The existing safety nets stay:** the 6% per second damage budget on boss health (`boss_dps_budget.md`), no regeneration, the power curve (20/40/60/80/100%), the Gates, the group scaling per floor, the telegraph that survives the Gates. A trait's damage cut is clamped at 60% whatever the setting.
7. **Two server settings (new, `nusmp-arc-server.toml`):** `tierPowers` (default true; false = the bosses fight with their plain moves only: no traits, no new statuses; the few retuned move numbers stay) and `tierPowerScale` (0.5 to 2.0, default 1.0, multiplies every trait's strength: radius, reduction, percentages, never a duration).

What the number does **not** see: the defensive and control traits (ARMOUR, UNTOUCHABLE, MIRROR, BALANCE, PULL, SYSTEM_LOCK, SIGHT, SLOW_FIELD, SHIELD_BREAK) and the status moves (slow, weaken, strip). Those make a fight longer or harder than the pressure says, so the bosses that carry the heaviest defences (Yammy, Charlos, Malty) were kept in the middle or low half of their band.

### Before and after (measured, 60% power, `TierBalance.table`)

| Tier F | before | after | | Tier D | before | after |
|---|---|---|---|---|---|---|
| Yammy Llargo | 3.42 | 3.00 | | "Big Mom" | 5.41 | 4.29 |
| Kurozumi Orochi | 3.35 | 3.09 | | Jugram | 4.58 | 3.86 |
| Charlos | 2.96 | 2.76 | | Rachel | 4.34 | 3.80 |
| Zetsu | 2.29 | 2.59 | | Bertholdt | 4.10 | 4.07 |
| Mr. 2 Bon Clay | 2.05 | 2.37 | | Shou Tucker | 3.82 | 3.93 |
| Sogou Nobuyuki | 1.96 | 2.40 | | Malty | 3.71 | 3.83 |
| **mean** | 2.67 | **2.70** | | Foxy | 3.20 | 3.71 |
| | | | | Morgan | 3.06 | 3.55 |
| | | | | Ao | 2.89 | 3.24 |
| | | | | Code | 2.87 | 3.40 |
| | | | | **mean** | 3.80 | **3.77** |

Band (±25%): **F 2.03 to 3.38**, **D 2.83 to 4.71**. All sixteen are inside (nearest to an edge: Orochi 0.29 under the F ceiling, Ao 0.41 over the D floor). Before, Yammy and Orochi (F) and "Big Mom" (D) were above the band and Sogou below. The bosses that were high came down with longer cooldowns and smaller hits (Big Mom, Bertholdt, Jugram, Rachel, Yammy, Orochi); the low ones got a little more damage or one more circle (Sogou, Bon Clay, Code, Ao).

Only the pressure at 60% power is banded. Late in the fight the climb and fusion steps push some bosses above their mean (at 100%: Big Mom 4.93, Bertholdt 4.62, Tucker 4.48, Rachel 4.06); that is the point of those traits and is not tuned away.

## 4. The sixteen

Each line: signature trait(s) (price), the moves that carry them or changed, and the counter / window. Move names are tagged N = named, T = theme, F = filler in the roster rows; the kit keeps five moves per boss and exactly one ultimate.

### Tier F (budget 5)

| Boss | Trait | Move changes | Counter / window |
|---|---|---|---|
| **Charlos** (One Piece) | UNTOUCHABLE (2) on Call the Marines | "Call the Marines" (T) is now an ordinary move: three guards arrive and he takes 35% less for 4 s; the ultimate is now Slave Collar Detonation (T), three circles that slow the marked | dodge the Marines, hit after the 4 s |
| **Zetsu** (Naruto) | SPORE_POISON (2) | Spore Cloud (T) now poisons instead of slowing; Flytrap Bite weakens; his hits poison | milk, leave the cloud |
| **Yammy Llargo** (Bleach) | ARMOUR (3), Hierro | Cero (N) bolt, Hierro Charge (T) rush, Ira: Cero Rampage (N) volley ultimate; all hits and cooldowns trimmed so the armour fits the band | magic damage, the open window after Ira |
| **Sogou Nobuyuki** (SAO) | PULL (2) on Gravity Spell | Gravity Spell (T) marks two circles and drags the marked; System Lock (T) is now a bolt that strips one beneficial effect | break line of sight, re-apply buffs |
| **Mr. 2 Bon Clay** (One Piece) | MIRROR (2) on Face Copy | Mane Mane: Face Copy (N) opens the 6 s reflect stance; Swan Volley (T) is wider with four feathers; Swan Kick and Ballet Flurry (T) | stop hitting for 6 s |
| **Kurozumi Orochi** (One Piece) | ECHO (2) on Eight Heads Strike | Eight Heads Strike (T) repeats once after 1.5 s (smaller volley, longer cooldown to pay for it); Venom Mist (F) now poisons | keep moving after the first volley |

### Tier D (budget 7)

| Boss | Trait | Move changes | Counter / window |
|---|---|---|---|
| **Malty S. Melromarc** (Shield Hero) | UNTOUCHABLE (2) + KARMA (2) | Call the Royal Guard (T) is now an ordinary move (three guards, 4 s shield); False Accusation (T) marks the target with Karma; the ultimate is now Slave Crest Curse (T), three circles that slow | milk clears the mark, hit after the guards |
| **Rachel** (Tower of God) | CLIMB (2) | Shinsu Bolt (T) is faster; Falling Star (F) and Star Volley (F) hit smaller but she speeds up each power step | kill her early |
| **Jugram Haschwalth** (Bleach) | BALANCE (3) on The Balance | Heilig Pfeil (N) bolt; Heilig Pfeil Barrage (T) volley; Hirenkyaku Dash (N); The Balance (N) is the ultimate and carries the weighing: its wind-up is the window | share the damage, pause while he raises the scales |
| **"Big Mom"** (One Piece) | SOUL_MARK (3) + FUSION (2) | Napoleon (N) cleave, Zeus (N) bolt, Prometheus (N) slam, Hungry Stomp (F) wave; Soul Pocus: Homies (N) is the volley ultimate and every bolt that lands adds a mark | break contact between marks, burst before the fusion |
| **Code** (Boruto) | KARMA (2) on Karma Burst + SIGHT (2) | Karma Burst (F) marks its target; Kara Seal (F) slows; every hit a little stronger to make up for no defence | milk, line of sight |
| **Bertholdt Hoover** (AoT) | STEAM (2) + FUSION (2) | Colossal Stomp (N); Steam Burst (N) ignites longer; Wall Fragments (T) volley is smaller | range, water, burst before the fusion |
| **Axe-Hand Morgan** (One Piece) | SHIELD_BREAK (1) + CLIMB (2) | Axe Hand Swing (N) disables a shield (as a move status too); Axe-Hand Whirl (T) 360 degrees ultimate | dodge instead of blocking |
| **Foxy** (One Piece) | SLOW_FIELD (2) + CLIMB (2) | Noro Noro Beam (N) slows harder; Slow-Slow Field (T) slam; Noro Noro Beam: Total Slow (N) is the volley ultimate | stay out of the ring, ranged |
| **Shou Tucker** (FMA) | FUSION (2) + SPORE_POISON (2) | Alexander the Chimera (T) swarm; Transmutation Circle (T); Chimera Rampage (T) ultimate; his hits poison | burst before the fusion, milk |
| **Ao** (Boruto) | SIGHT (2) + SYSTEM_LOCK (3) | Byakugan Strike (T) marked circles; Palm Strike (F); Palm Barrage (F) ultimate; every hit strips one effect | break line of sight, re-apply buffs |

All 16 fit their tier budget (F: 2 to 3 of 5, D: 2 to 5 of 7, never more than two traits). Each boss keeps its floors, size, loot and look.

## 5. What this costs (rule 11)

Worst case, with `tierBossMax` (default 6, at most 24) tier bosses alive on the whole server and a crowd around each:

- **Server work per tick:** nothing per tick except two cheap checks (an `isEmpty` on the trait list and a time comparison). Once a second per boss (offset by the boss's id, so they do not all fire in the same tick): one foe query for the slow field and the sight, at most 24 foes each, so at most **144 foe checks per second** at the default cap; the state map is pruned.
- **Particles:** the slow-field ring is 16 single-particle packets per second per boss, the stance ring 12, drawn only while the trait is on and only to players in range; the other traits draw nothing of their own. Tell shapes are the existing `Tells` ones (a ring is `points` particles, a burst or gather one call, a line at most 64).
- **Delayed tasks:** at most 4 pull impulses and 2 echo tasks per use of the bound move, each a `later(ticks, Runnable)` on the boss (it does nothing if the boss is dead or removed by then).
- **Memory:** five small maps per boss (at most 24 players each, oldest evicted), pruned once a second; every entry runs out by itself (marks and Karma 15 s, the steam and lock gaps 2 s, the Balance window a few seconds), so a player who logs out, dies or leaves the dimension simply expires; everything is cleared when the fight ends and when the boss dies or leaves the world.
- **Network:** no packets of its own: statuses ride the vanilla effect sync, the fire and glowing too.
- **Entities, blocks, chunks:** none spawned, changed or loaded by a trait. The guard wisps are the existing swarm move.
- **Client:** no new drawing, no client classes touched; all state is server side and keyed by player UUID.
- **Failures:** every trait hook runs in a try/catch; one failure is logged once per hook and the boss carries on with its plain moves. `tierPowers` false skips all of it.

## 6. What is not included, and what I need from you

- **No player skills yet** (decision 1). Players do not get a boss's power in this release.
- **Not seen in a game.** The balance is measured by the simulator and held by tests, not play-tested; the live traits are covered by the server self-test (`tier.power.*`, which summons real bosses with FakePlayers) but nobody has fought them. The numbers are meant to be retuned from one place (`TierRosterF` / `TierRosterD` rows, `TierBalance` bands, the two settings).
- **Defence is not in the number** (section 3): if a fight with Yammy or the guard-calling bosses feels too long, lower `tierPowerScale` first.
- **Charlos and Malty changed shape:** their guard call is now an ordinary move and the ultimate is a new circle-slow move (an ultimate may not carry Untouchable). The old moves are still there under the same ids.
- **Anime accuracy** is from memory (tags above) until the wikis can be fetched.
- **Light Yagami, the Zogratis bosses and the 22 new bosses of `roster.md`** are untouched; the new bosses get their powers the same way when they are built.

Decisions for the owner:
1. Should each boss also drop its power as a **1-of-1 player skill** (the claim registry of `RULES.md`)? Proposed as its own release after this one.
2. Are the **budgets (F 5, D 7) and the ±25% band** right, or should the fights be harder or easier than today? One number each.
3. Is a **35% armour / shield** acceptable for Yammy and the guard-calling bosses, or should defence traits always be window-only?
