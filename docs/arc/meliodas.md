# Meliodas, the Dragon's Sin of Wrath (boss, tier 3)

Why this page exists: Meliodas is the deepest of the three bosses of the first Sins group (`docs/arc/sdsa.md`), and his fight is built around ONE lesson, what a
player should throw at a boss. This page says who he is in the series (and how sure I am), what the fight does and why, every move with its number and its
counter, what the config and the quest hooks are, the weapon he would give (not built), and what is not verified. Nothing here has run in a game.

## 1. Who he is (canon, with the limits of what I could read)

The pages could not be opened (the fetch tool was refused by the environment), so every fact below comes from SEARCH SUMMARIES of these pages, read on
2026-10-17, and from memory of the series; where only fan wikis or content farms said it, it is marked weak.

* Dragon's Sin of Wrath, captain of the Seven Deadly Sins; short, boyish, messy blond hair, emerald eyes, a dragon mark on the upper left arm (the sources
  disagree on the exact place and design). Sources: the Looper explainer
  <https://www.looper.com/720355/meliodass-powers-from-the-seven-deadly-sins-explained/>, <https://en.wikipedia.org/wiki/Meliodas>,
  <https://en.wikipedia.org/wiki/List_of_The_Seven_Deadly_Sins_characters>, the fan wiki <https://nanatsu-no-taizai.fandom.com/wiki/Meliodas> (search summary only).
* **Full Counter**: reflects MAGIC aimed at him back at the caster; consistent across sources that it does not answer direct (physical) attacks. A fan wiki says
  "twice the power"; no canon source for a fixed multiplier was found (so this build does not amplify, see 3.A). One profile says he cannot initiate attacks while
  using it (not modelled: the stance here is a flag he fights through).
* **Hellblaze**: thick black flame, "virtually impossible to extinguish", said to stop regeneration and healing; wrapped round the blade for stronger strikes
  (one fan source names the technique **Kamichigiri** for that).
* **Lostvayne**: his Sacred Treasure, a sword; its clone ability ("Jitsuzo Bunshin", real-image clones) complemented Full Counter.
* **Demon Mark / Assault Mode**: the mark grows and his powers rise at the cost of his humanity; Assault Mode is the strongest and brings back his demonic
  personality. What his hair and clothes do in Assault Mode could not be confirmed.
* Weakly sourced or not found: the "Hellblaze: Purgatory" technique name (remembered, not confirmed by the pages I could read).

## 2. The record (`SdsCharacters.MELIODAS`)

Saga `SEVEN_DEADLY_SINS`, tier 3. 3200 health, attack 20, speed 0.36, armour 14, toughness 8 (the top of the tier-3 band and under Lucius's 3600). Floors 60
to 90 on either road (he is the deep one). Favoured themes `flame`, `infernal_foundry`, `abyss`, `blood`. After the four Eye of the Midnight Sun bosses
(`fana`, `patolli`, `rades_spirito`, `rhya`), open in any order with Escanor and Ban. Colours 0x7A1432 (crimson), accent 0xFF5E6E. Lines: opening, gates, death
in his voice (in the record). Magic family `demon_fire` (existing id).

## 3. The fight: three ideas, each with a counter

### A. Full Counter (`FullCounter`, pure)

What it does: while the stance is up (50 to 80 ticks, every 300 to 380 ticks), a SPELL that hits him from his front half (a cone of 200 degrees) is swallowed (he
takes nothing) and a share of it hits the caster as damage of the Tensura type `reflected`. "Spell" means vanilla magic damage or any Tensura damage type that is
not physical (`SpellSeal.isSpellDamage`), minus the anti-magic blows (`DanteMoves.antiMagicHit`).

Why it is shaped like this: the owner asked for fights that "transcend standard stat checks" and counter the player's own ultimate tricks. The commonest trick
against a boss is magic, so he answers magic. But a boss that reflects every spell is unbeatable for a caster and a one-shot for the caster whose own spell
comes back, so the rule has limits, all of them in `FullCounter` and all of them tested:

| limit | number | why |
|---|---|---|
| only spells | steel, arrows, thrown things, anti-magic pass | the series: it does not answer direct attacks; the party always has an answer in hand |
| only his front | a cone of 100 degrees each side; his images give all sides | going behind him is the reward for knowing the rule |
| one answer is at most 30% of the caster's maximum health, and at least 3% | `VICTIM_CAP`, `FLOOR_SHARE` | a reflection is never a kill, and a feeble spell still stings |
| one stance takes at most 55% of one caster's maximum health in all | `VICTIM_BUDGET` | no stance can empty a player |
| one answer per caster per 6 ticks | `REPEAT_GAP` | a spell that hits every tick is answered once per gap, not once per tick |
| the stance breaks after it has swallowed 16/18/20/26% of his maximum health in magic | `DemonKingMath.breakFraction` (512 to 832 health points at 3200) | the counter to a pile of magic is more magic than it can hold, or none |
| the damage comes back as it was | `REFLECT_RATIO` 1.0 | the series (fan wiki) says amplified; here the caps, not a multiplier, keep it fair |

He raises it by himself when the party starts to cast: three spell hits within 80 ticks make `chooseMove` put Full Counter first (`counter.pressed`). He cannot
raise it in Assault Mode (see B), and `meliodasFullCounter=false` removes it altogether.

How the party beats it: attack with steel, arrows, thrown weapons or anti-magic (a demon sword, the Anti-Magic Lord) while it is up; go round to his back; wait
it out (the pane in front of him shows it and a line on the action bar says it); or overload it deliberately (the stance shatters, he is open for 30 ticks and the
caster who did it is credited with `sds.full_counter_broken`).

### B. The Demon Mark ladder and Assault Mode (`DemonKingMath`, pure)

The framework gives every boss five power steps (20, 40, 60, 80, 100). For Meliodas they are the Captain (20), the Demon Mark wakes (40), it spreads (60),
**Assault Mode** (80 until the Gates) and the **Demon King** (the Gates, 100):

| power | mark | his own damage factor | speed bonus | Full Counter | images | rush dashes | Hellblaze field radius / ticks / burn / cling |
|---|---|---|---|---|---|---|---|
| 20 | 0 | 1.00 | 0 | 50 ticks, cooldown 380, breaks at 16% | 0 | 0 | 3.40 / 80 / 2.4 / 40 |
| 40 | 1 | 1.04 | 0 | 60 / 360 / 18% | 0 | 0 | 3.85 / 92 / 2.8 / 50 |
| 60 | 2 | 1.10 | +3% | 70 / 330 / 20% | 3 | 0 | 4.30 / 104 / 3.2 / 60 |
| 80 | 3 | 1.22 | +12% | NONE (he cannot raise it) | 3 | 3 | 4.75 / 116 / 3.6 / 70 |
| 100 | 4 | 1.30 | +8% | 80 / 300 / 26% | 4 | 4 | 5.20 / 128 / 4.0 / 80 |

Assault Mode is the window the party was promised: he gives up his guard for speed, so he cannot raise Full Counter, and he takes 12% more from every blow
(`DemonKingMath.takenFactor`, applied in `filterDamage`). The chat says so. At the Gates he guards again, longer and harder to break, and the Wrath (C) comes.
These numbers multiply the framework's own power tuning (damage 0.6 to 1.6, cooldowns 1.0 to 0.42, wind-ups 1.0 to 0.75), they do not replace it.

### C. Hellblaze and the Demon King's Wrath

* **Hellblaze** is a field of black flame (drawn dark crimson, because a black that is really black cannot be seen). It burns whoever stands in it every half
  second and keeps burning them a few seconds after they leave (the series: it is not put out; here leaving is the only way, and it fades in time, not in space).
* **The Wrath** (the Gates' ultimate, `demon_king_wrath`): he channels (60 ticks, 45 at the Gates, at least 30) and **swallows the magic thrown at him**: it grows
  the ring (7 blocks, up to 11 when 30% of his maximum health in magic has been fed to it) and does nothing to him. Everything else counts toward BREAKING it:
  12% of his maximum health in blows during the channel and the 60 ticks of lock that follow (the ring stands still and red) breaks the nova, staggers him for
  80 ticks (+25% damage taken, nothing new starts) and credits `sds.nova_broken`. Unbroken, it goes off: 26 base damage up to 40 when fed, a shield or
  anti-magic cuts it, everything inside is thrown out. The ring can always be walked out of in the lock (`DemonKingMath.walkable`, tested: 11 blocks in 3 s at
  the walking pace of 4.3 blocks a second). After the channel (the lock) magic lands normally and also counts toward the break.

  Why: the second half of the lesson. Full Counter says "do not spam magic"; the Wrath says "and do not feed it"; both are answered by physical damage, which
  is what a pure caster party has least of, and both give a clear, readable window to the party that learns.

## 4. The moves (`MeliodasTable`, numbers before the boss's damage law)

The scheduler is the framework's (power gates, ranges, cooldowns, mana, ultimate pacing); `MeliodasBoss.chooseMove` only adds: nothing starts while he is
staggered or the nova is locked; Full Counter is offered only when the stance is ready, the config allows it and he is not in Assault Mode, and comes first when
the party casts at him; the images wait for a free stance. Wind-up and cooldown are in ticks, range in blocks. Every move has a telegraph of at least 16 ticks
(12 at the Gates, the framework shortens it) and the ultimates at least 30; the aim, the mark or the ring is locked at the start of the wind-up and drawn for the
whole of it, so moving is always the answer. Damage types are Tensura's, so the victim's resistances to them count (`aura_slash` for the sword, `black_flame`).

| move (id) | from power | wind-up / cooldown / recovery | range | weight | base damage | what it is | counter |
|---|---|---|---|---|---|---|---|
| Lostvayne Slash (`lostvayne_slash`) | 20 | 16 / 60 / 8 | 0 to 4.8 | 6 | 14 | a cone of 116 degrees, reach 4.6 | leave the cone, back off, or get behind him |
| Lostvayne Lunge (`lostvayne_lunge`) | 20 | 16 / 120 / 10 | 4.5 to 16 | 4 | 16 | a dash along a strip 2.6 wide, up to 14 long, stopping a body short | step out of the strip |
| Lostvayne Wave (`lostvayne_wave`) | 20 | 18 / 110 / 8 | 6 to 32 | 5 | 8 each | 1 to 3 low crescents (by the power's cast slots) along the lines drawn | step out of the lines |
| Full Counter (`full_counter`) | 20 | 16 / 340 / 8 | any | 3 | none | raises the stance (3.A) | 3.A |
| Hellblaze (`hellblaze`) | 40 | 22 / 240 / 10 | 4 to 30 | 4 (crowd) | burn 2.4 to 4.0 per half second | a Hellblaze field on the target (two at the Gates) | leave the ring, and wait out the cling |
| Kamichigiri (`kamichigiri`) | 40 | 20 / 170 / 12 | 0 to 6.5 | 4 | 24 | a long, heavy cut: cone of 72 degrees, reach 6.4 | leave the cone or go behind him |
| Demon Step (`demon_step`) | 40 | 16 / 190 / 8 | 5 to 26 | 3 | 15 | gone, and cutting in a ring of 3.2 behind the target's mark | leave the marked ring |
| Hellblaze Burst (`hellblaze_burst`) | 40 | 20 / 200 / 12 | 0 to 9 | 3 (crowd) | 14 | a dome of black flame round him, radius 5.0 to 6.2, shoves outward | leave the radius; cover cuts it to 40% |
| Lostvayne: Real Image (`lostvayne_images`) | 60 | 26 / 520 / 14 | 0 to 40 | 3 (crowd) | 12 each | 3 (4) images on a ring of 5 round the target cut across the ring in turn, 8 ticks apart; he guards ALL sides meanwhile | stand between the lines, away from the middle; steel through his guard |
| Assault Rush (`assault_rush`) | 80 | 18 / 300 / 16 | 3 to 26 | 4 (crowd) | 13 each | 3 (4) dashes, 12 ticks apart, through the marked spots | stay off the lines |
| Hellblaze: Purgatory (`hellblaze_purgatory`, ultimate) | 60 | 58 / 1300 / 60 | 0 to 40 | 5 (crowd) | 17 | columns of black flame (5 to 7, by the power's cast slots) on the foes and round the target, each leaving a short field | leave the rings; a shield or anti-magic cuts it; he is exposed after |
| Demon King's Wrath (`demon_king_wrath`, ultimate) | 100 | 60 / 1500 / 60 | 0 to 36 | 5 | 26 to 40 | the nova (3.C) | break it with steel, or leave the ring |

After an ultimate he is exposed for 60 ticks (the framework: +25% damage, nothing new starts).

## 5. Config and quest keys

`nusmp-arc-sdsa-server.toml`, section `[sdsa]` (shared by the three; the lead registers it): `meliodasFullCounter` true. The three other groups of keys are on the
Escanor and Ban pages. Quest hook keys (`NusmpEvents.fire(player, key)`, the lead wires advancements): `sds.full_counter_broken` (the player's spell hit broke the
stance), `sds.nova_broken` (the player's blow broke the Wrath).

## 6. The model (art kit, `tools/arc/char_meliodas.py`)

A short man (1.84 blocks, the sources say about five feet), blond hair, emerald eyes, an open dark vest over a plain shirt, the sword Lostvayne with a crimson
glow; the second form is the Demon King (the mark across his face and arms, blazing eyes, a black lock in the hair, small horns, the blade burning). The colours,
the black lock and the horns are for the game and are not from a source. ArcLook (from `tools/arc/manifest.json`): `("meliodas", 1.84, 1.06, 1.88, 0.40, 1.08, 0.71, true)`,
hitbox 0.62 x 1.84; both forms are the same size. Preview PNGs (looked at; they are kept outside the repository, in the job's scratchpad folder `previews/sdsa/`): `arc_meliodas_front.png`, `_face`, `_idle`,
`_walk`, `_attack`, `_cast_big`, `_transform`, `_death`, `arc_meliodas_form2_front.png`, `_form2_face`, `_form2_idle`, `_form2_attack`, `_form2_cast_big`.

## 7. The Charter Grimoire (chance 0.30)

Eight real Tensura skills (checked against the skill list): `reflector` (the Full Counter), `wrath` (the rage that grows), `black_flame` and `hellfire` (Hellblaze),
`demon_lord_haki` (the Demon King's presence), `aura_sword` (Lostvayne in hand), `strengthen_body`, `darkness_attack_resistance`. Nothing the Charter forbids.

## 8. The weapon he would give (NOT built; owner rule: Seven Deadly Sins weapons are a later job)

**Lostvayne**, his Sacred Treasure. It would be a real 3D model in hand (rule 7) with a crimson glow and a black-flame effect, with the Tensura engravings
`severance` 2 (it splits what it cuts), `barrier_piercing` 1, `magic_interference` 1 (the Full Counter's cousin: it spoils spells aimed at the bearer) and
`elemental_boost` 2 (Hellblaze), to be added to `item/WeaponEngravings.java`. The kit prop in the boss model (`sword` with a crimson glow) is only the boss's.

## 9. Tests and what is NOT verified

Pure and tested (JUnit 5, 94 tests for the three bosses, run in the local shim; the 19 mutants of the pure classes were all caught): `FullCounterTest`,
`DemonKingMathTest`, `MeliodasTableTest` (with the fight simulation `SdsaFightSim` driving the real `MoveScheduler`), `SdsCharactersTest`.
Type-checked against the API stubs: `MeliodasBoss`, `MeliodasMoves`, `SdsaStrikes` (only the known stub artifact remains: the missing `deserializeNBT` on an entity
subclass).

Not verified, honestly: nothing has run in a game. Every number is a first guess and needs a smoke test, above all the damage numbers (they go through the balance
law, so the real figures depend on the player's EP), the size of the reflected damage, whether the three spell hits that make him raise the stance are too few
for a party that uses multi-hit spells, whether `SpellSeal.isSpellDamage` classes the party's spells as spells (it says every Tensura type that is not tagged
physical is a spell), the visual effects (shapes borrowed from other characters, tinted: `SLASH_WAVE`, `DARK_IAI`, `FIRE_PILLAR`, `FIRE_WILD`, `MIRROR_PANE`,
`NIHILITY_ZONE` and others; their look is unseen), and the names of the moves that are mine (`lostvayne_lunge`, `lostvayne_wave`, `demon_step`, `hellblaze_burst`,
`assault_rush`, `demon_king_wrath`). Kamichigiri and "Real Image" follow what the search summaries said; what Kamichigiri really does in the series is
not confirmed.
