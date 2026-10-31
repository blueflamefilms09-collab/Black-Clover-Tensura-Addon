# Tier boss powers and how they are balanced (design, 3.39)

Status: **proposal for the owner**. Nothing here is built yet. It answers: "give the bosses their power / abilities from their anime, and tell me what power and how to balance them".

Scope: the **16 light tier bosses** of 3.33.0 (six F, ten D). Light Yagami already is his anime power (the Death Note scene) and is not touched. The Zogratis bosses, the arcs' bosses and the SDS / Black Clover characters are not touched.

Sources (rule 10): the English and Japanese wikis could **not** be fetched from the build environment, so every power below is from memory and tagged **named** (the series' own name), **theme** (something the character really does, shaped and named by me) or **filler** (invented so the fight has enough to do). Nothing is wiki-verified; correct a character in one place (its roster row) once the hosts are allowed.

## 1. What a "power" is here

Today every one of the 16 has five moves cut from nine generic shapes (cleave, slam, wave, bolt, volley, marked, rush, beam, swarm). That is why Foxy, Ao and Code feel like the same boss. A power adds two things on top, both small and bounded:

1. **A signature trait** (rule 2 of `RULES.md`): one passive or phase rule from the character's anime, with a telegraph and a counter the player can learn.
2. **Moves re-cut to the anime**: same five slots, but the shapes, statuses and names follow the character (four new statuses are appended to `TierMove.Effect`: POISON, PULL, SHIELD_BREAK, STRIP).

Not in this release: the **1-of-1 player skill** each boss is meant to drop (rule 1 of `RULES.md`, claim registry, +10% skill gain). That is a separate, larger release (decision 1 at the end).

## 2. The trait catalogue (all server side, all capped)

Every trait is data (a `TierTrait` on the row) run by one class, `TierTraits`, from the hooks the boss framework already has (`bossTick`, `mitigate`, `onPowerChanged`, `onGatesOpened`). None spawns an entity, changes a block or heals. A trait looks at the same at-most-24 foes a move does, at most once per second.

| Trait | Price | What it does | The counter |
|---|---|---|---|
| SLOW_FIELD | 2 | Players within R (6, 9 at the Gates) get Slowness I | stay outside R and use range |
| STEAM | 2 | Players within 5 blocks that hit him in melee catch fire for 3 s (once per 2 s) | range, water, fire resistance |
| ARMOUR (Hierro) | 3 | Melee (non-magic) damage -35%; gone for 6 s after his ultimate | magic damage, wait for the open window |
| BALANCE | 3 | Every 12 s the player who dealt the most damage in the window is weakened 6 s; a lone player takes 15% of the window back (capped) | share damage, pause before the tick |
| SOUL_MARK | 3 | Each of his hits on a player adds a mark (max 3, 15 s each); at 3 the player is weakened and he hits 10% harder for 8 s | break contact, let the marks run out |
| MIRROR | 2 | For 6 s after the copy move he reflects 25% of the damage he takes (max 6 per hit) | stop hitting for the 6 s, or use ranged chip |
| SYSTEM_LOCK | 3 | Hits strip up to 2 beneficial effects; the ultimate strips all in 20 blocks | re-apply after the ultimate |
| PULL | 2 | A marked player is dragged toward him (0.4 blocks per impulse) for 3 s | break line of sight, heavy knockback resistance |
| SPORE_POISON | 2 | His hits poison (never lethal: stops at 1 health) | milk, regeneration |
| FUSION | 2 | At 66% and 33% health: +15% size, +15% damage, +10% speed (permanent) | burst before the fusion |
| SIGHT | 2 | Sees through invisibility and sneaking; Gentle Fist hits weaken | break line of sight with blocks |
| KARMA | 2 | His mark: +20% damage taken from him for 15 s | milk clears it |
| ECHO | 2 | His volley repeats once after 1.5 s | keep moving after the first volley |
| UNTOUCHABLE | 2 | -35% damage for 4 s after he calls his guards | dodge the guards, hit after the window |
| CLIMB | 2 | Each power step: +8% speed and +8% damage (max 3 steps) | kill pace; he is slowest at the start |
| SHIELD_BREAK | 1 | His axe cleaves disable a raised shield for 5 s | do not block those, dodge |

Budget (points a boss may spend on traits): **F 5, D 7** (+2 per letter later: C 9, B 11, A 13). One boss may carry one or two traits, never more than the budget. Nothing is stackable, no trait is shared by two bosses in the same tier with the same numbers.

## 3. How they are balanced (the method)

Seven layers, from the one the owner already tuned to the new knob:

1. **Tier stats** (existing, `TierLetter`): F 700 health / 10 attack, D 1000 / 13; a row scales them a little.
2. **Pressure index (new, measured):** the expected damage a boss deals per second. Computed by `tools/tier_bosses/tier_pressure.py` (and, once built, the pure `TierBalance` class and its test) by simulating the real scheduler: one move at a time, weighted pick, cooldowns, a 50% hit rate (60% for the ultimate), the ultimate leaving him exposed. **Rule: inside a tier every boss lies within ±25% of the tier's mean at 60% power**, traits that deal damage (steam, poison, burn) counted in. A boss with a strong defensive trait is allowed to sit at the low end (-25%), one with no trait at the high end.
3. **Hit size caps:** an ordinary move deals at most **1.6x the boss's attack**, an ultimate at most **2.0x** (checked by the test; today the largest are 1.3x and 1.5x, so the moves have room to grow a little but never to spike).
4. **Trait budget (new):** the points above. A boss with 3-point armour has fewer points left for a second trait.
5. **A counter for every trait, and a window:** a trait that blocks damage always has an open window (after the ultimate, after the guards fall) and a damage type that goes round it. The test refuses a trait without both.
6. **The existing safety nets stay:** the 6% per second damage budget on boss health (`boss_dps_budget.md`), no regeneration, the power curve (20/40/60/80/100%), the Gates, the group scaling per floor, the telegraph that survives the Gates (>= 12 ticks, 30 for the ultimate).
7. **Two server settings (new):** `tierPowers` (default true; false restores the plain 3.37 moves, nothing else changes) and `tierPowerScale` (0.5 to 2.0, default 1.0, multiplies every trait's strength: radius, reduction, percentages, never a duration).

### Where they stand today (measured, before any power)

Expected damage per second at 60% power, 50% hit rate (`tier_pressure.py`):

| Tier F | pressure | | Tier D | pressure |
|---|---|---|---|---|
| Yammy Llargo | 3.44 | | "Big Mom" | **5.29** |
| Kurozumi Orochi | 3.41 | | Jugram | 4.42 |
| Charlos | 2.76 | | Rachel | 4.18 |
| Zetsu | 2.30 | | Bertholdt | 3.89 |
| Mr. 2 Bon Clay | 2.08 | | Shou Tucker | 3.74 |
| Sogou Nobuyuki | **1.92** | | Malty | 3.66 |
| mean | 2.65 | | Foxy | 3.21 |
| | | | Morgan | 3.04 |
| | | | Ao | 2.88 |
| | | | Code | **2.83** |
| | | | mean | 3.71 |

Band (±25%): F 1.99-3.31, D 2.78-4.64. Out of band today: **Yammy and Orochi (above)**, **Sogou (below)** in F; **"Big Mom" (above)** in D. The release brings them in: Big Mom's Hungry Stomp and Napoleon get longer cooldowns, Yammy's Heavy Fist and Orochi's volley the same; Sogou and Bon Clay get one more damaging move each. The final numbers are printed in the release notes and held by the test.

## 4. The sixteen

Each line: signature trait (price) and the moves that change. Move names tagged N = named, T = theme, F = filler. The kit keeps five moves per boss and exactly one ultimate.

### Tier F (budget 5)

| Boss | Trait | Move changes | Counter / window |
|---|---|---|---|
| **Charlos** (One Piece) | UNTOUCHABLE (2) | "Call the Marines" (T): the guards arrive in a ring, he is shielded 4 s; Slave Collar Detonation (T) stays, slowing the marked | dodge the Marines, hit after 4 s |
| **Zetsu** (Naruto) | SPORE_POISON (2) | Spore Cloud (T) now poisons; Earth Ambush (T) strikes from below at the marked circle; White Zetsu Clones (T) | milk, leave the cloud |
| **Yammy Llargo** (Bleach) | ARMOUR / Hierro (3) | Cero (N) keeps its beam; Ira: Cero Rampage (N) now opens the armour for 6 s; Hierro Charge (T) | magic damage, the open window after Ira |
| **Sogou Nobuyuki** (SAO) | PULL (2) | Gravity Spell (T) pulls the marked player; System Lock (T) strips 1 beneficial effect; Wing Dash (F) | break line of sight, re-apply buffs |
| **Mr. 2 Bon Clay** (One Piece) | MIRROR (2) | Mane Mane: Face Copy (N) = 6 s reflect stance; Swan Kick (T), Ballet Flurry (T) | stop hitting for 6 s |
| **Kurozumi Orochi** (One Piece) | ECHO (2) | Eight Heads Strike (T) repeats once; Venom Mist (F) poisons | keep moving after the first volley |

### Tier D (budget 7)

| Boss | Trait | Move changes | Counter / window |
|---|---|---|---|
| **Malty S. Melromarc** (Shield Hero) | UNTOUCHABLE (2) + KARMA (2) | False Accusation (T) = Karma mark; Slave Crest Curse (T) slows; Call the Royal Guard (T) shields her 4 s | milk clears the mark, hit after the guards |
| **Rachel** (Tower of God) | CLIMB (2) | Shinsu Bolt (T) fast bolts; Falling Star (F) | kill her early, she speeds up each step |
| **Jugram Haschwalth** (Bleach) | BALANCE (3) | Heilig Pfeil (N) volley; Hirenkyaku Dash (N); The Balance (N) = ultimate that opens him for 4 s | share damage, hit in the open window |
| **"Big Mom"** (One Piece) | SOUL_MARK (3) + FUSION (2) | Napoleon (N) cleave, Zeus (N) bolt, Prometheus (N) slam; Soul Pocus: Homies (N) = ultimate that adds marks to everyone in range | break contact between marks, burst before the fusion |
| **Code** (Boruto) | KARMA (2) + SIGHT (2) | Karma Burst (F) marks; Kara Seal (F) | milk, line of sight |
| **Bertholdt Hoover** (AoT) | STEAM (2) + FUSION (2) | Colossal Stomp (N), Steam Burst (N) now ignites; Wall Fragments (T) | range, water, burst before the fusion |
| **Axe-Hand Morgan** (One Piece) | SHIELD_BREAK (1) + CLIMB (2) | Axe Hand Swing (N) disables a shield; Axe-Hand Whirl (T) 360 degrees | dodge instead of blocking |
| **Foxy** (One Piece) | SLOW_FIELD (2) + CLIMB (2) | Noro Noro Beam (N) slows hard; Total Slow (N) = ultimate that doubles the field for 4 s | stay out of the field, ranged |
| **Shou Tucker** (FMA) | FUSION (2) + SPORE_POISON (2) | Alexander the Chimera (T) swarm; Transmutation Circle (T); Chimera Rampage (T) ultimate | burst before the fusion, milk |
| **Ao** (Boruto) | SIGHT (2) + SYSTEM_LOCK (3 = Gentle Fist, strips 1) | Byakugan Strike (T); Palm Barrage (F) | break line of sight, re-apply buffs |

All 16 fit their tier budget (F: 2-3 of 5, D: 4-7 of 7). Each boss keeps its floors, size, loot and look.

## 5. What this costs (rule 11)

- Worst case per boss: one trait pass per second over at most 24 foes (the move limit), at most 3 status applications per pass; no packets of its own (statuses use vanilla effect sync); no entity, block or chunk work; marks are a map of at most 24 UUIDs, cleared on death, logout, dimension change and when the boss leaves the world. At most `tierBossMax` (default 6) bosses are alive, so the server pass is at most 144 foe checks per second. Client: no new drawing.
- Multiplayer: all state is server side and keyed by player UUID; a trait never reads `Minecraft` or client classes.

## 6. What is not included, and what I need from you

- **No player skills yet** (decision 1). Players do not get a boss's power in this release.
- **Not seen in a game.** Balance is measured by the simulator and held by tests, not play-tested; the numbers are meant to be retuned from one place (`TierRosterF` / `TierRosterD` rows, `TierBalance` bands, the two settings).
- **Anime accuracy** is from memory (tags above) until the wikis can be fetched.
- **Light Yagami, the Zogratis bosses and the 22 new bosses of `roster.md`** are untouched; the new bosses get their powers the same way when they are built.

Decisions for the owner:
1. Should each boss also drop its power as a **1-of-1 player skill** (the claim registry of `RULES.md`)? Proposed as its own release after this one.
2. Are the **budgets (F 5, D 7) and the +-25% band** right, or should the fights be harder or easier than today? One number each.
3. Is a **-35% armour / shield** acceptable for Yammy and the guard-shielded bosses, or should defence traits always be window-only?
